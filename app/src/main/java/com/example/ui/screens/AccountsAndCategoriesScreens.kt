package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.AccountBalanceSummary
import com.example.domain.FinanceCalculator
import com.example.ui.LeovaneyUiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.GlassTopBar
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassBorderWhite15
import com.example.ui.theme.GlassBorderWhite28
import com.example.ui.theme.GlassSurfaceWhite12
import com.example.ui.theme.GlassSurfaceWhite8
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber

@Composable
fun AccountsScreen(
    uiState: LeovaneyUiState,
    onBack: () -> Unit,
    onSaveAccount: (AccountEntity, () -> Unit) -> Unit,
    onToggleArchiveAccount: (AccountEntity) -> Unit,
    onSaveTransfer: (TransactionEntity, () -> Unit) -> Unit
) {
    BackHandler(onBack = onBack)
    val currency = uiState.preferences.currencyCode
    var editingAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detailAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTransferDialog by rememberSaveable { mutableStateOf(false) }

    if (editingAccountId != null) {
        val existing = uiState.accounts.find { it.id == editingAccountId }
        AccountFormDialog(
            initial = existing,
            isSaving = uiState.isSaving,
            onDismiss = { editingAccountId = null },
            onSave = { acc ->
                onSaveAccount(acc) { editingAccountId = null }
            }
        )
    }

    if (showTransferDialog) {
        val activeAccounts = uiState.accounts.filter { !it.isArchived }
        TransferBetweenAccountsDialog(
            accounts = activeAccounts.ifEmpty { uiState.accounts },
            isSaving = uiState.isSaving,
            onDismiss = { showTransferDialog = false },
            onSaveTransfer = { tx ->
                onSaveTransfer(tx) { showTransferDialog = false }
            }
        )
    }

    if (detailAccountId != null) {
        val summary = uiState.accountBalances.find { it.account.id == detailAccountId }
        if (summary != null) {
            val accountTxs = uiState.allTransactions.filter {
                it.accountId == summary.account.id || it.destinationAccountId == summary.account.id
            }
            AccountDetailDialog(
                summary = summary,
                transactions = accountTxs,
                currencyCode = currency,
                onEdit = {
                    detailAccountId = null
                    editingAccountId = summary.account.id
                },
                onToggleArchive = {
                    onToggleArchiveAccount(summary.account)
                    detailAccountId = null
                },
                onDismiss = { detailAccountId = null }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("accounts_screen")
    ) {
        GlassTopBar(
            title = "Rekening & Dompet",
            subtitle = "Kelola tunai, bank, e-wallet, dan tabungan",
            onBackClick = onBack,
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showTransferDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceWhite8)
                            .border(1.dp, GlassBorderWhite15, CircleShape)
                            .testTag("transfer_account_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer Antar-Rekening", tint = PureWhite)
                    }
                    IconButton(
                        onClick = { editingAccountId = 0L },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .testTag("add_account_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Rekening", tint = ObsidianBlack)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(borderColor = GlassBorderWhite28) {
                    Text("TOTAL SALDO REKENING AKTIF", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FinanceCalculator.formatRupiah(uiState.netWorth.activeAccountsTotal, currency),
                        style = MaterialTheme.typography.displayMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Transfer antar-rekening mengurangi saldo sumber dan menambah saldo tujuan secara konsisten tanpa terhitung sebagai pemasukan/pengeluaran baru.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                }
            }

            items(uiState.accountBalances, key = { it.account.id }) { summary ->
                val acc = summary.account
                GlassCard(
                    onClick = { detailAccountId = acc.id },
                    modifier = Modifier.testTag("account_card_${acc.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = acc.type.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSilver,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(GlassSurfaceWhite8)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                if (acc.isArchived) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Arsip",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningAmber
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saldo Awal: ${FinanceCalculator.formatRupiah(acc.initialBalance, currency)} • Dibuat ${FinanceCalculator.formatDate(acc.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSilver
                            )
                        }
                        Text(
                            text = FinanceCalculator.formatRupiah(summary.currentBalance, currency),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = JetBrainsMonoFamily,
                            color = if (summary.currentBalance >= 0L) PureWhite else ExpenseRed
                        )
                    }
                    if (acc.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(acc.note, style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountFormDialog(
    initial: AccountEntity?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: AccountType.BANK) }
    var initialBalanceStr by rememberSaveable { mutableStateOf(initial?.initialBalance?.toString() ?: "0") }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedCharcoal,
        title = { Text(if (initial == null) "Tambah Rekening" else "Edit Rekening", color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = "Nama Rekening / Dompet",
                    placeholder = "Contoh: BCA Utama / GoPay",
                    testTag = "account_name_input"
                )
                Text("Jenis Rekening", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                GlassTextField(
                    value = initialBalanceStr,
                    onValueChange = { initialBalanceStr = it.filter { ch -> ch.isDigit() || ch == '-' } },
                    label = "Saldo Awal (Rp)",
                    isNumeric = true,
                    testTag = "account_initial_balance_input"
                )
                GlassTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Catatan Opsional",
                    testTag = "account_note_input"
                )
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                }
            }
        },
        confirmButton = {
            GlassPrimaryButton(
                text = "Simpan",
                isLoading = isSaving,
                onClick = {
                    if (name.trim().isEmpty()) {
                        error = "Nama rekening wajib diisi."
                        return@GlassPrimaryButton
                    }
                    val initBal = initialBalanceStr.toLongOrNull() ?: 0L
                    onSave(
                        AccountEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            type = type,
                            initialBalance = initBal,
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                            note = note.trim(),
                            isArchived = initial?.isArchived ?: false
                        )
                    )
                },
                testTag = "save_account_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = MutedSilver) }
        }
    )
}

@Composable
private fun TransferBetweenAccountsDialog(
    accounts: List<AccountEntity>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveTransfer: (TransactionEntity) -> Unit
) {
    var fromId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var toId by rememberSaveable { mutableStateOf(accounts.drop(1).firstOrNull()?.id ?: accounts.firstOrNull()?.id ?: 1L) }
    var amountStr by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedCharcoal,
        title = { Text("Transfer Antar-Rekening", color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dari Rekening Sumber", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = fromId == acc.id,
                            onClick = { fromId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }
                Text("Ke Rekening Tujuan", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = toId == acc.id,
                            onClick = { toId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }
                GlassTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { ch -> ch.isDigit() }; error = null },
                    label = "Nominal Transfer (Rp)",
                    isNumeric = true,
                    testTag = "transfer_amount_input"
                )
                GlassTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Catatan Transfer"
                )
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                }
            }
        },
        confirmButton = {
            GlassPrimaryButton(
                text = "Transfer Sekarang",
                isLoading = isSaving,
                onClick = {
                    val amt = amountStr.toLongOrNull() ?: 0L
                    if (amt <= 0L) {
                        error = "Nominal transfer harus lebih dari Rp0."
                        return@GlassPrimaryButton
                    }
                    if (fromId == toId) {
                        error = "Rekening sumber dan tujuan tidak boleh sama."
                        return@GlassPrimaryButton
                    }
                    onSaveTransfer(
                        TransactionEntity(
                            type = TransactionType.TRANSFER,
                            amount = amt,
                            accountId = fromId,
                            destinationAccountId = toId,
                            title = "Transfer Antar-Rekening",
                            note = note.trim()
                        )
                    )
                },
                testTag = "confirm_transfer_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = MutedSilver) }
        }
    )
}

@Composable
private fun AccountDetailDialog(
    summary: AccountBalanceSummary,
    transactions: List<TransactionEntity>,
    currencyCode: String,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDismiss: () -> Unit
) {
    val acc = summary.account
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedCharcoal,
        title = { Text("${acc.name} (#${acc.id})", color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Saldo Saat Ini: ${FinanceCalculator.formatRupiah(summary.currentBalance, currencyCode)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = PureWhite
                )
                Text("Jenis: ${acc.type.label}", color = MutedSilver)
                Text("Saldo Awal: ${FinanceCalculator.formatRupiah(acc.initialBalance, currencyCode)}", color = MutedSilver)
                Text("Total Pemasukan: +${FinanceCalculator.formatRupiah(summary.totalIncome, currencyCode)}", color = IncomeGreen)
                Text("Total Pengeluaran: -${FinanceCalculator.formatRupiah(summary.totalExpense, currencyCode)}", color = ExpenseRed)
                Text("Transfer Masuk: +${FinanceCalculator.formatRupiah(summary.transferIn, currencyCode)}", color = TransferBlue)
                Text("Transfer Keluar: -${FinanceCalculator.formatRupiah(summary.transferOut, currencyCode)}", color = TransferBlue)
                Text("Riwayat Transaksi: ${transactions.size} catatan", color = PureWhite, fontWeight = FontWeight.SemiBold)
                transactions.take(4).forEach { tx ->
                    Text(
                        text = "• ${tx.title}: ${FinanceCalculator.formatRupiah(tx.amount, currencyCode)} (${FinanceCalculator.formatDate(tx.timestamp, "dd MMM")})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onToggleArchive) {
                    Icon(
                        imageVector = if (acc.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (acc.isArchived) "Aktifkan" else "Arsipkan", color = WarningAmber)
                }
                TextButton(onClick = onEdit) {
                    Text("Edit", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = MutedSilver) }
        }
    )
}

@Composable
fun CategoriesScreen(
    uiState: LeovaneyUiState,
    onBack: () -> Unit,
    onSaveCategory: (CategoryEntity, () -> Unit) -> Unit,
    onToggleArchiveCategory: (CategoryEntity) -> Unit
) {
    BackHandler(onBack = onBack)
    var editingCategory by rememberSaveable { mutableStateOf<Long?>(null) }
    var filterType by rememberSaveable { mutableStateOf<CategoryType?>(null) }

    if (editingCategory != null) {
        val existing = uiState.categories.find { it.id == editingCategory }
        var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
        var type by rememberSaveable { mutableStateOf(existing?.type ?: CategoryType.EXPENSE) }
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingCategory = null },
            containerColor = ElevatedCharcoal,
            title = { Text(if (existing == null) "Tambah Kategori" else "Edit Kategori", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = "Nama Kategori",
                        testTag = "category_name_input"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryType.entries.forEach { ct ->
                            FilterChip(
                                selected = type == ct,
                                onClick = { type = ct },
                                label = { Text(ct.label) }
                            )
                        }
                    }
                    if (error != null) {
                        Text(error!!, style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                    }
                }
            },
            confirmButton = {
                GlassPrimaryButton(
                    text = "Simpan",
                    isLoading = uiState.isSaving,
                    onClick = {
                        if (name.trim().isEmpty()) {
                            error = "Nama kategori wajib diisi."
                            return@GlassPrimaryButton
                        }
                        onSaveCategory(
                            CategoryEntity(
                                id = existing?.id ?: 0L,
                                name = name.trim(),
                                type = type,
                                isArchived = existing?.isArchived ?: false
                            )
                        ) { editingCategory = null }
                    },
                    testTag = "save_category_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) { Text("Batal", color = MutedSilver) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("categories_screen")
    ) {
        GlassTopBar(
            title = "Kategori Pemasukan & Pengeluaran",
            subtitle = "${uiState.categories.size} kategori terdaftar",
            onBackClick = onBack,
            trailingContent = {
                IconButton(
                    onClick = { editingCategory = 0L },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .testTag("add_category_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Kategori", tint = ObsidianBlack)
                }
            }
        )

        val visibleCategories = uiState.categories.filter { filterType == null || it.type == filterType }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { filterType = null },
                        label = { Text("Semua") }
                    )
                    CategoryType.entries.forEach { ct ->
                        FilterChip(
                            selected = filterType == ct,
                            onClick = { filterType = ct },
                            label = { Text(ct.label) }
                        )
                    }
                }
            }

            items(visibleCategories, key = { it.id }) { cat ->
                GlassCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (cat.isArchived) MutedSilver else PureWhite
                            )
                            Text(
                                text = "${cat.type.label}${if (cat.isArchived) " • Diarsipkan" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (cat.type == CategoryType.INCOME) IncomeGreen else ExpenseRed
                            )
                        }
                        Row {
                            IconButton(onClick = { editingCategory = cat.id }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Kategori", tint = PureWhite)
                            }
                            IconButton(onClick = { onToggleArchiveCategory(cat) }) {
                                Icon(
                                    imageVector = if (cat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = "Arsipkan Kategori",
                                    tint = MutedSilver
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetsScreen(
    uiState: LeovaneyUiState,
    onNavigateToCategories: () -> Unit,
    onSaveBudget: (BudgetEntity, () -> Unit) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit
) {
    val currency = uiState.preferences.currencyCode
    var editingBudgetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteCandidate by rememberSaveable { mutableStateOf<Long?>(null) }

    if (editingBudgetId != null) {
        val existing = uiState.budgets.find { it.id == editingBudgetId }
        val expenseCategories = uiState.categories.filter { it.type == CategoryType.EXPENSE && !it.isArchived }
        var selectedCategoryId by rememberSaveable { mutableStateOf(existing?.categoryId) }
        var limitStr by rememberSaveable { mutableStateOf(existing?.amountLimit?.toString() ?: "") }
        var thresholdStr by rememberSaveable { mutableStateOf(existing?.alertThresholdPercent?.toString() ?: "80") }
        var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingBudgetId = null },
            containerColor = ElevatedCharcoal,
            title = { Text(if (existing == null) "Atur Anggaran Bulanan" else "Edit Anggaran", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Periode: ${uiState.selectedBudgetYearMonth}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                    Text("Cakupan Anggaran", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Total Bulanan") },
                            modifier = Modifier.testTag("budget_scope_total")
                        )
                        expenseCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                modifier = Modifier.testTag("budget_scope_cat_${cat.id}")
                            )
                        }
                    }
                    GlassTextField(
                        value = limitStr,
                        onValueChange = { limitStr = it.filter { ch -> ch.isDigit() }; error = null },
                        label = "Batas Pengeluaran (Rp)",
                        isNumeric = true,
                        testTag = "budget_limit_input"
                    )
                    GlassTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it.filter { ch -> ch.isDigit() } },
                        label = "Peringatan Saat Mencapai (%)",
                        isNumeric = true
                    )
                    GlassTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Catatan Opsional"
                    )
                    if (error != null) {
                        Text(error!!, style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                    }
                }
            },
            confirmButton = {
                GlassPrimaryButton(
                    text = "Simpan",
                    isLoading = uiState.isSaving,
                    onClick = {
                        val limit = limitStr.toLongOrNull() ?: 0L
                        if (limit <= 0L) {
                            error = "Batas anggaran harus lebih besar dari Rp0."
                            return@GlassPrimaryButton
                        }
                        val threshold = (thresholdStr.toIntOrNull() ?: 80).coerceIn(10, 100)
                        onSaveBudget(
                            BudgetEntity(
                                id = existing?.id ?: 0L,
                                categoryId = selectedCategoryId,
                                yearMonth = uiState.selectedBudgetYearMonth,
                                amountLimit = limit,
                                alertThresholdPercent = threshold,
                                note = note.trim()
                            )
                        ) { editingBudgetId = null }
                    },
                    testTag = "save_budget_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { editingBudgetId = null }) { Text("Batal", color = MutedSilver) }
            }
        )
    }

    if (deleteCandidate != null) {
        val target = uiState.budgets.find { it.id == deleteCandidate }
        if (target != null) {
            ConfirmDeleteDialog(
                title = "Hapus Anggaran?",
                message = "Apakah Anda yakin ingin menghapus batas anggaran ini?",
                onConfirm = { onDeleteBudget(target) },
                onDismiss = { deleteCandidate = null }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("budgets_screen")
    ) {
        GlassTopBar(
            title = "Anggaran",
            subtitle = "Kontrol batas pengeluaran bulan ${uiState.selectedBudgetYearMonth}",
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onNavigateToCategories,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceWhite8)
                            .border(1.dp, GlassBorderWhite15, CircleShape)
                            .testTag("manage_categories_button")
                    ) {
                        Icon(Icons.Default.Category, contentDescription = "Kelola Kategori", tint = PureWhite)
                    }
                    IconButton(
                        onClick = { editingBudgetId = 0L },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .testTag("add_budget_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Anggaran", tint = ObsidianBlack)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard {
                    Text(
                        text = "Perhitungan anggaran didasarkan pada transaksi pengeluaran aktual pada bulan berjalan. Transfer antar-rekening tidak dihitung sebagai pengeluaran anggaran.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                }
            }

            if (uiState.budgetSummaries.isEmpty()) {
                item {
                    GlassCard(borderColor = GlassBorderWhite28) {
                        Text(
                            text = "Belum Ada Anggaran Bulan Ini",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tetapkan batas pengeluaran bulanan keseluruhan atau per kategori (Makanan, Transportasi, Belanja, dll).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSilver
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        GlassPrimaryButton(
                            text = "Buat Anggaran Pertama",
                            onClick = { editingBudgetId = 0L },
                            testTag = "empty_create_budget_button"
                        )
                    }
                }
            } else {
                items(uiState.budgetSummaries, key = { it.budget.id }) { summary ->
                    val statusColor = when {
                        summary.isExceeded -> ExpenseRed
                        summary.isNearLimit -> WarningAmber
                        else -> IncomeGreen
                    }
                    GlassCard(
                        borderColor = if (summary.isExceeded) ExpenseRed else if (summary.isNearLimit) WarningAmber else GlassBorderWhite15,
                        modifier = Modifier.testTag("budget_card_${summary.budget.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = summary.categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            Text(
                                text = "${summary.usagePercentage}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = statusColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(GlassSurfaceWhite12)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (summary.usagePercentage / 100f).coerceIn(0.02f, 1f))
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(statusColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Terpakai", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                                Text(
                                    text = FinanceCalculator.formatRupiah(summary.spentAmount, currency),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = PureWhite
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Batas Anggaran", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                                Text(
                                    text = FinanceCalculator.formatRupiah(summary.limitAmount, currency),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = MutedSilver
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (summary.remainingAmount >= 0L) "Sisa Anggaran" else "Melebihi Batas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSilver
                                )
                                Text(
                                    text = FinanceCalculator.formatRupiah(kotlin.math.abs(summary.remainingAmount), currency),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = statusColor
                                )
                            }
                        }
                        if (summary.isExceeded || summary.isNearLimit) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (summary.isExceeded) {
                                        "Peringatan: Pengeluaran telah melampaui batas anggaran!"
                                    } else {
                                        "Perhatian: Pemakaian anggaran mendekati batas (${summary.budget.alertThresholdPercent}%)."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { editingBudgetId = summary.budget.id }) {
                                Text("Edit", color = MutedSilver)
                            }
                            TextButton(onClick = { deleteCandidate = summary.budget.id }) {
                                Text("Hapus", color = ExpenseRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
