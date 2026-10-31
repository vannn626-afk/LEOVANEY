package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryType
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.FinanceCalculator
import com.example.ui.LeovaneyUiState
import com.example.ui.TransactionFilterState
import com.example.ui.TransactionSortOrder
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.GlassTopBar
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassBorderWhite15
import com.example.ui.theme.GlassBorderWhite28
import com.example.ui.theme.GlassSurfaceWhite8
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TransferBlue

@Composable
fun TransactionsScreen(
    uiState: LeovaneyUiState,
    openEditorInitially: Boolean = false,
    onConsumeInitialEditor: () -> Unit = {},
    onUpdateFilter: ((TransactionFilterState) -> TransactionFilterState) -> Unit,
    onResetFilter: () -> Unit,
    onSaveTransaction: (TransactionEntity, () -> Unit) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    val currency = uiState.preferences.currencyCode
    var editingTransaction by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.autoSaver()) {
        mutableStateOf<Long?>(if (openEditorInitially) 0L else null)
    }
    var selectedDetailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAdvancedFilter by rememberSaveable { mutableStateOf(false) }

    if (openEditorInitially && editingTransaction == null) {
        editingTransaction = 0L
        onConsumeInitialEditor()
    }

    val activeAccounts = uiState.accounts.filter { !it.isArchived }
    val accountMap = uiState.accounts.associateBy { it.id }
    val categoryMap = uiState.categories.associateBy { it.id }

    if (editingTransaction != null) {
        val existing = uiState.allTransactions.find { it.id == editingTransaction }
        TransactionFormDialog(
            initial = existing,
            accounts = activeAccounts.ifEmpty { uiState.accounts },
            categories = uiState.categories.filter { !it.isArchived },
            isSaving = uiState.isSaving,
            onDismiss = {
                editingTransaction = null
                onConsumeInitialEditor()
            },
            onSave = { tx ->
                onSaveTransaction(tx) {
                    editingTransaction = null
                    onConsumeInitialEditor()
                }
            }
        )
    }

    if (selectedDetailId != null) {
        val detailTx = uiState.allTransactions.find { it.id == selectedDetailId }
        if (detailTx != null) {
            TransactionDetailDialog(
                tx = detailTx,
                accountName = accountMap[detailTx.accountId]?.name ?: "Rekening",
                destinationName = detailTx.destinationAccountId?.let { accountMap[it]?.name },
                categoryName = detailTx.categoryId?.let { categoryMap[it]?.name } ?: "-",
                currencyCode = currency,
                onEdit = {
                    selectedDetailId = null
                    editingTransaction = detailTx.id
                },
                onDelete = {
                    selectedDetailId = null
                    deleteCandidateId = detailTx.id
                },
                onDismiss = { selectedDetailId = null }
            )
        }
    }

    if (deleteCandidateId != null) {
        val target = uiState.allTransactions.find { it.id == deleteCandidateId }
        if (target != null) {
            ConfirmDeleteDialog(
                title = "Hapus Transaksi?",
                message = "Menghapus \"${target.title}\" (${FinanceCalculator.formatRupiah(target.amount, currency)}) akan otomatis memperbarui saldo rekening, anggaran, dan laporan.",
                onConfirm = { onDeleteTransaction(target) },
                onDismiss = { deleteCandidateId = null }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("transactions_screen")
    ) {
        GlassTopBar(
            title = "Transaksi",
            subtitle = "${uiState.filteredTransactions.size} transaksi ditampilkan",
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showAdvancedFilter = !showAdvancedFilter },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceWhite8)
                            .border(1.dp, GlassBorderWhite15, CircleShape)
                            .testTag("toggle_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter transaksi",
                            tint = PureWhite
                        )
                    }
                    IconButton(
                        onClick = { editingTransaction = 0L },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .testTag("add_transaction_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Transaksi",
                            tint = ObsidianBlack
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search & Quick Type Filter
            item {
                GlassCard {
                    GlassTextField(
                        value = uiState.transactionFilter.searchQuery,
                        onValueChange = { q -> onUpdateFilter { it.copy(searchQuery = q) } },
                        label = "Cari transaksi (keterangan, catatan, nominal)",
                        testTag = "transaction_search_input"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.transactionFilter.selectedType == null,
                            onClick = { onUpdateFilter { it.copy(selectedType = null) } },
                            label = { Text("Semua Jenis") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = ObsidianBlack
                            )
                        )
                        TransactionType.entries.forEach { tType ->
                            FilterChip(
                                selected = uiState.transactionFilter.selectedType == tType,
                                onClick = { onUpdateFilter { it.copy(selectedType = tType) } },
                                label = { Text(tType.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PureWhite,
                                    selectedLabelColor = ObsidianBlack
                                ),
                                modifier = Modifier.testTag("filter_type_${tType.name}")
                            )
                        }
                    }

                    if (showAdvancedFilter) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Urutkan Berdasarkan", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TransactionSortOrder.entries.forEach { order ->
                                FilterChip(
                                    selected = uiState.transactionFilter.sortOrder == order,
                                    onClick = { onUpdateFilter { it.copy(sortOrder = order) } },
                                    label = { Text(order.label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Filter Rekening", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.transactionFilter.selectedAccountId == null,
                                onClick = { onUpdateFilter { it.copy(selectedAccountId = null) } },
                                label = { Text("Semua Rekening") }
                            )
                            uiState.accounts.forEach { acc ->
                                FilterChip(
                                    selected = uiState.transactionFilter.selectedAccountId == acc.id,
                                    onClick = { onUpdateFilter { it.copy(selectedAccountId = acc.id) } },
                                    label = { Text(acc.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Filter Kategori", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.transactionFilter.selectedCategoryId == null,
                                onClick = { onUpdateFilter { it.copy(selectedCategoryId = null) } },
                                label = { Text("Semua Kategori") }
                            )
                            uiState.categories.forEach { cat ->
                                FilterChip(
                                    selected = uiState.transactionFilter.selectedCategoryId == cat.id,
                                    onClick = { onUpdateFilter { it.copy(selectedCategoryId = cat.id) } },
                                    label = { Text(cat.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = uiState.transactionFilter.filterByPeriod,
                                onClick = { onUpdateFilter { it.copy(filterByPeriod = !it.filterByPeriod) } },
                                label = { Text("Hanya Bulan Ini") }
                            )
                            TextButton(onClick = onResetFilter) {
                                Text("Reset Filter", color = PureWhite)
                            }
                        }
                    }
                }
            }

            if (uiState.filteredTransactions.isEmpty()) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MutedSilver,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tidak ada transaksi yang cocok",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tambahkan transaksi baru atau ubah kata kunci pencarian Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSilver
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            GlassPrimaryButton(
                                text = "Tambah Transaksi",
                                onClick = { editingTransaction = 0L },
                                testTag = "empty_add_tx_button"
                            )
                        }
                    }
                }
            } else {
                items(uiState.filteredTransactions, key = { it.id }) { tx ->
                    val accName = accountMap[tx.accountId]?.name ?: "Rekening"
                    val destName = tx.destinationAccountId?.let { accountMap[it]?.name ?: "Tujuan" }
                    val catName = tx.categoryId?.let { categoryMap[it]?.name } ?: tx.type.label
                    val subtitle = if (tx.type == TransactionType.TRANSFER) {
                        "Transfer: $accName → $destName • ${FinanceCalculator.formatDateTime(tx.timestamp)}"
                    } else {
                        "$catName • $accName • ${FinanceCalculator.formatDateTime(tx.timestamp)}"
                    }
                    GlassCard(
                        onClick = { selectedDetailId = tx.id },
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.testTag("tx_card_${tx.id}")
                    ) {
                        DashboardTransactionRow(
                            title = tx.title,
                            subtitle = subtitle,
                            amount = tx.amount,
                            type = tx.type,
                            currencyCode = currency
                        )
                        if (tx.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Catatan: ${tx.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSilver,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { editingTransaction = tx.id },
                                modifier = Modifier.testTag("edit_tx_${tx.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MutedSilver)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", color = MutedSilver)
                            }
                            TextButton(
                                onClick = { deleteCandidateId = tx.id },
                                modifier = Modifier.testTag("delete_tx_${tx.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = ExpenseRed)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hapus", color = ExpenseRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionFormDialog(
    initial: TransactionEntity?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(initial?.type ?: TransactionType.EXPENSE) }
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var amountStr by rememberSaveable { mutableStateOf(initial?.amount?.toString() ?: "") }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var selectedAccountId by rememberSaveable {
        mutableStateOf(initial?.accountId ?: accounts.firstOrNull()?.id ?: 1L)
    }
    var selectedDestAccountId by rememberSaveable {
        mutableStateOf(
            initial?.destinationAccountId
                ?: accounts.drop(1).firstOrNull()?.id
                ?: accounts.firstOrNull()?.id
        )
    }

    val matchingCategories = categories.filter {
        when (selectedType) {
            TransactionType.INCOME -> it.type == CategoryType.INCOME
            TransactionType.EXPENSE -> it.type == CategoryType.EXPENSE
            TransactionType.TRANSFER -> false
        }
    }

    var selectedCategoryId by rememberSaveable {
        mutableStateOf(initial?.categoryId ?: matchingCategories.firstOrNull()?.id)
    }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedCharcoal,
        title = {
            Text(
                text = if (initial == null) "Tambah Transaksi" else "Edit Transaksi",
                style = MaterialTheme.typography.titleLarge,
                color = PureWhite
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Transaction Type Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionType.entries.forEach { tType ->
                        val active = selectedType == tType
                        val activeColor = when (tType) {
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.TRANSFER -> TransferBlue
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) activeColor.copy(alpha = 0.22f) else GlassSurfaceWhite8)
                                .border(
                                    1.dp,
                                    if (active) activeColor else GlassBorderWhite15,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedType = tType
                                    val newCats = categories.filter {
                                        if (tType == TransactionType.INCOME) it.type == CategoryType.INCOME
                                        else it.type == CategoryType.EXPENSE
                                    }
                                    selectedCategoryId = newCats.firstOrNull()?.id
                                }
                                .padding(vertical = 10.dp)
                                .testTag("tx_type_tab_${tType.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tType.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) activeColor else MutedSilver,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                GlassTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it.filter { ch -> ch.isDigit() }
                        localError = null
                    },
                    label = "Nominal (Rp)",
                    placeholder = "Contoh: 150000",
                    isNumeric = true,
                    testTag = "tx_amount_input"
                )

                GlassTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        localError = null
                    },
                    label = "Keterangan",
                    placeholder = "Contoh: Makan siang / Gaji bulanan",
                    testTag = "tx_title_input"
                )

                // Source Account selector
                Text(
                    text = if (selectedType == TransactionType.TRANSFER) "Rekening Sumber" else "Rekening",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSilver
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) },
                            modifier = Modifier.testTag("tx_source_acc_${acc.id}")
                        )
                    }
                }

                if (selectedType == TransactionType.TRANSFER) {
                    Text(
                        text = "Rekening Tujuan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSilver
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedDestAccountId == acc.id,
                                onClick = { selectedDestAccountId = acc.id },
                                label = { Text(acc.name) },
                                modifier = Modifier.testTag("tx_dest_acc_${acc.id}")
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSilver
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        matchingCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                modifier = Modifier.testTag("tx_cat_${cat.id}")
                            )
                        }
                    }
                }

                GlassTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Catatan Opsional",
                    placeholder = "Tambahkan detail jika diperlukan",
                    singleLine = false,
                    testTag = "tx_note_input"
                )

                if (localError != null) {
                    Text(
                        text = localError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed,
                        modifier = Modifier.testTag("tx_form_error")
                    )
                }
            }
        },
        confirmButton = {
            GlassPrimaryButton(
                text = "Simpan",
                isLoading = isSaving,
                onClick = {
                    val amount = amountStr.toLongOrNull() ?: 0L
                    if (amount <= 0L) {
                        localError = "Nominal harus lebih besar dari Rp0."
                        return@GlassPrimaryButton
                    }
                    if (title.trim().isEmpty()) {
                        localError = "Keterangan transaksi wajib diisi."
                        return@GlassPrimaryButton
                    }
                    if (selectedType == TransactionType.TRANSFER && selectedAccountId == selectedDestAccountId) {
                        localError = "Rekening sumber dan tujuan tidak boleh sama."
                        return@GlassPrimaryButton
                    }
                    onSave(
                        TransactionEntity(
                            id = initial?.id ?: 0L,
                            type = selectedType,
                            amount = amount,
                            categoryId = if (selectedType == TransactionType.TRANSFER) null else selectedCategoryId,
                            accountId = selectedAccountId,
                            destinationAccountId = if (selectedType == TransactionType.TRANSFER) selectedDestAccountId else null,
                            timestamp = initial?.timestamp ?: System.currentTimeMillis(),
                            title = title.trim(),
                            note = note.trim()
                        )
                    )
                },
                testTag = "save_transaction_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = MutedSilver)
            }
        }
    )
}

@Composable
private fun TransactionDetailDialog(
    tx: TransactionEntity,
    accountName: String,
    destinationName: String?,
    categoryName: String,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedCharcoal,
        title = { Text("Detail Transaksi", color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = FinanceCalculator.formatRupiah(tx.amount, currencyCode),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = PureWhite
                )
                Text("Keterangan: ${tx.title}", color = PureWhite)
                Text("Jenis: ${tx.type.label}", color = MutedSilver)
                if (tx.type == TransactionType.TRANSFER) {
                    Text("Dari Rekening: $accountName", color = MutedSilver)
                    Text("Ke Rekening: ${destinationName ?: "-"}", color = MutedSilver)
                } else {
                    Text("Kategori: $categoryName", color = MutedSilver)
                    Text("Rekening: $accountName", color = MutedSilver)
                }
                Text("Waktu: ${FinanceCalculator.formatDateTime(tx.timestamp)}", color = MutedSilver)
                if (tx.note.isNotBlank()) {
                    Text("Catatan: ${tx.note}", color = MutedSilver)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Hapus", color = ExpenseRed)
                }
                TextButton(onClick = onEdit) {
                    Text("Edit", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = MutedSilver)
            }
        }
    )
}
