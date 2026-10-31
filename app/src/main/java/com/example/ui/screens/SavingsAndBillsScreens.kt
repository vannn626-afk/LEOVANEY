package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.data.local.BillCategoryType
import com.example.data.local.BillEntity
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionType
import com.example.domain.FinanceCalculator
import com.example.ui.LeovaneyUiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.GlassTopBar
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassSurfaceWhite12
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningAmber

@Composable
fun SavingsGoalsScreen(
    uiState: LeovaneyUiState,
    onBack: () -> Unit,
    onSaveGoal: (SavingsGoalEntity, () -> Unit) -> Unit,
    onAddContribution: (Long, Long, Boolean, String, () -> Unit) -> Unit,
    onToggleGoalCompleted: (SavingsGoalEntity) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit
) {
    BackHandler(onBack = onBack)
    val currency = uiState.preferences.currencyCode
    var editingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var contributionGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isWithdrawalMode by rememberSaveable { mutableStateOf(false) }
    var deleteGoalId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (editingGoalId != null) {
        val existing = uiState.savingsGoals.find { it.id == editingGoalId }
        var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
        var targetStr by rememberSaveable { mutableStateOf(existing?.targetAmount?.toString() ?: "") }
        var currentStr by rememberSaveable { mutableStateOf(existing?.currentAmount?.toString() ?: "0") }
        var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingGoalId = null },
            containerColor = ElevatedCharcoal,
            title = { Text(if (existing == null) "Tambah Target Tabungan" else "Edit Target Tabungan", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = "Nama Target Tabungan",
                        placeholder = "Contoh: Dana Darurat / Laptop Baru",
                        testTag = "savings_name_input"
                    )
                    GlassTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it.filter { ch -> ch.isDigit() }; error = null },
                        label = "Nominal Target (Rp)",
                        isNumeric = true,
                        testTag = "savings_target_input"
                    )
                    if (existing == null) {
                        GlassTextField(
                            value = currentStr,
                            onValueChange = { currentStr = it.filter { ch -> ch.isDigit() } },
                            label = "Saldo Terkumpul Awal (Rp)",
                            isNumeric = true
                        )
                    }
                    GlassTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Catatan / Tenggat Waktu Opsional"
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
                        val target = targetStr.toLongOrNull() ?: 0L
                        if (name.trim().isEmpty()) {
                            error = "Nama target tidak boleh kosong."
                            return@GlassPrimaryButton
                        }
                        if (target <= 0L) {
                            error = "Nominal target harus lebih besar dari Rp0."
                            return@GlassPrimaryButton
                        }
                        val curr = existing?.currentAmount ?: (currentStr.toLongOrNull() ?: 0L)
                        onSaveGoal(
                            SavingsGoalEntity(
                                id = existing?.id ?: 0L,
                                name = name.trim(),
                                targetAmount = target,
                                currentAmount = curr,
                                note = note.trim(),
                                isCompleted = curr >= target
                            )
                        ) { editingGoalId = null }
                    },
                    testTag = "save_savings_goal_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { editingGoalId = null }) { Text("Batal", color = MutedSilver) }
            }
        )
    }

    if (contributionGoalId != null) {
        val goal = uiState.savingsGoals.find { it.id == contributionGoalId }
        if (goal != null) {
            var amountStr by rememberSaveable { mutableStateOf("") }
            var note by rememberSaveable { mutableStateOf("") }
            var error by rememberSaveable { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { contributionGoalId = null },
                containerColor = ElevatedCharcoal,
                title = {
                    Text(
                        text = if (isWithdrawalMode) "Catat Penarikan: ${goal.name}" else "Tambah Kontribusi: ${goal.name}",
                        color = PureWhite
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Pencatatan kontribusi tabungan hanya memperbarui progres target tanpa menggandakan pemasukan baru pada laporan keseluruhan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSilver
                        )
                        GlassTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it.filter { ch -> ch.isDigit() }; error = null },
                            label = "Nominal (Rp)",
                            isNumeric = true,
                            testTag = "contribution_amount_input"
                        )
                        GlassTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = "Catatan Kontribusi"
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
                            val amt = amountStr.toLongOrNull() ?: 0L
                            if (amt <= 0L) {
                                error = "Nominal harus lebih besar dari Rp0."
                                return@GlassPrimaryButton
                            }
                            onAddContribution(goal.id, amt, isWithdrawalMode, note.trim()) {
                                contributionGoalId = null
                            }
                        },
                        testTag = "save_contribution_button"
                    )
                },
                dismissButton = {
                    TextButton(onClick = { contributionGoalId = null }) { Text("Batal", color = MutedSilver) }
                }
            )
        }
    }

    if (deleteGoalId != null) {
        val target = uiState.savingsGoals.find { it.id == deleteGoalId }
        if (target != null) {
            ConfirmDeleteDialog(
                title = "Hapus Target Tabungan?",
                message = "Target \"${target.name}\" beserta riwayat kontribusinya akan dihapus.",
                onConfirm = { onDeleteGoal(target) },
                onDismiss = { deleteGoalId = null }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("savings_screen")
    ) {
        GlassTopBar(
            title = "Target Tabungan",
            subtitle = "Pantau progres dana impian & dana darurat",
            onBackClick = onBack,
            trailingContent = {
                IconButton(
                    onClick = { editingGoalId = 0L },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .testTag("add_savings_goal_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Target", tint = ObsidianBlack)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.savingsGoals.isEmpty()) {
                item {
                    GlassCard {
                        Text("Belum Ada Target Tabungan", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Buat target tabungan untuk memantau progres dana darurat, liburan, atau pembelian aset tanpa penghitungan ganda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSilver
                        )
                    }
                }
            } else {
                items(uiState.savingsGoals, key = { it.id }) { goal ->
                    val progress = if (goal.targetAmount > 0L) {
                        ((goal.currentAmount.toFloat() / goal.targetAmount.toFloat()) * 100f).coerceIn(0f, 100f)
                    } else 0f
                    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0L)
                    val goalContribs = uiState.savingsContributions.filter { it.goalId == goal.id }

                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(goal.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                Text(
                                    text = if (goal.isCompleted) "Selesai Tercapai" else "Sisa menuju target: ${FinanceCalculator.formatRupiah(remaining, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (goal.isCompleted) IncomeGreen else MutedSilver
                                )
                            }
                            Text(
                                text = "${String.format("%.0f", progress)}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMonoFamily,
                                color = if (goal.isCompleted) IncomeGreen else PureWhite
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
                                    .fillMaxWidth(fraction = (progress / 100f).coerceIn(0.02f, 1f))
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (goal.isCompleted) IncomeGreen else PureWhite)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Terkumpul: ${FinanceCalculator.formatRupiah(goal.currentAmount, currency)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = IncomeGreen
                            )
                            Text(
                                "Target: ${FinanceCalculator.formatRupiah(goal.targetAmount, currency)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = MutedSilver
                            )
                        }
                        if (goalContribs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Riwayat Kontribusi Terbaru:", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                            goalContribs.take(3).forEach { c ->
                                val sign = if (c.isWithdrawal) "-" else "+"
                                Text(
                                    text = "• $sign${FinanceCalculator.formatRupiah(c.amount, currency)} (${FinanceCalculator.formatDate(c.timestamp, "dd MMM")}) ${c.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (c.isWithdrawal) ExpenseRed else IncomeGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = {
                                        isWithdrawalMode = false
                                        contributionGoalId = goal.id
                                    },
                                    modifier = Modifier.testTag("add_contrib_btn_${goal.id}")
                                ) {
                                    Text("+ Kontribusi", color = IncomeGreen, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = {
                                        isWithdrawalMode = true
                                        contributionGoalId = goal.id
                                    }
                                ) {
                                    Text("- Tarik", color = WarningAmber)
                                }
                            }
                            Row {
                                IconButton(onClick = { onToggleGoalCompleted(goal) }) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Tandai Selesai",
                                        tint = if (goal.isCompleted) IncomeGreen else MutedSilver
                                    )
                                }
                                IconButton(onClick = { editingGoalId = goal.id }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PureWhite)
                                }
                                IconButton(onClick = { deleteGoalId = goal.id }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = ExpenseRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillsAndRecurringScreen(
    uiState: LeovaneyUiState,
    onBack: () -> Unit,
    onSaveBill: (BillEntity, () -> Unit) -> Unit,
    onPayBill: (BillEntity, Long?) -> Unit,
    onDeleteBill: (BillEntity) -> Unit,
    onSaveRecurring: (RecurringTransactionEntity, () -> Unit) -> Unit,
    onToggleRecurring: (RecurringTransactionEntity) -> Unit,
    onDeleteRecurring: (RecurringTransactionEntity) -> Unit
) {
    BackHandler(onBack = onBack)
    val currency = uiState.preferences.currencyCode
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0 = Tagihan, 1 = Berulang
    var showAddBill by rememberSaveable { mutableStateOf(false) }
    var showAddRecurring by rememberSaveable { mutableStateOf(false) }

    if (showAddBill) {
        var name by rememberSaveable { mutableStateOf("") }
        var catType by rememberSaveable { mutableStateOf(BillCategoryType.LISTRIK) }
        var amountStr by rememberSaveable { mutableStateOf("") }
        var freq by rememberSaveable { mutableStateOf(RecurrenceFrequency.MONTHLY) }
        var note by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddBill = false },
            containerColor = ElevatedCharcoal,
            title = { Text("Tambah Tagihan", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = "Nama Tagihan",
                        placeholder = "Contoh: PLN Rumah / WiFi Indihome",
                        testTag = "bill_name_input"
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BillCategoryType.entries.forEach { bt ->
                            FilterChip(
                                selected = catType == bt,
                                onClick = { catType = bt },
                                label = { Text(bt.label) }
                            )
                        }
                    }
                    GlassTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { ch -> ch.isDigit() }; error = null },
                        label = "Nominal Tagihan (Rp)",
                        isNumeric = true,
                        testTag = "bill_amount_input"
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurrenceFrequency.entries.forEach { f ->
                            FilterChip(
                                selected = freq == f,
                                onClick = { freq = f },
                                label = { Text(f.label) }
                            )
                        }
                    }
                    GlassTextField(value = note, onValueChange = { note = it }, label = "Catatan")
                    if (error != null) Text(error!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                GlassPrimaryButton(
                    text = "Simpan",
                    isLoading = uiState.isSaving,
                    onClick = {
                        val amt = amountStr.toLongOrNull() ?: 0L
                        if (name.trim().isEmpty() || amt <= 0L) {
                            error = "Nama dan nominal (> 0) wajib diisi."
                            return@GlassPrimaryButton
                        }
                        onSaveBill(
                            BillEntity(
                                name = name.trim(),
                                categoryType = catType,
                                amount = amt,
                                dueDate = System.currentTimeMillis() + 3L * 86400_000L,
                                frequency = freq,
                                note = note.trim()
                            )
                        ) { showAddBill = false }
                    },
                    testTag = "save_bill_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddBill = false }) { Text("Batal", color = MutedSilver) }
            }
        )
    }

    if (showAddRecurring) {
        var title by rememberSaveable { mutableStateOf("") }
        var amountStr by rememberSaveable { mutableStateOf("") }
        var freq by rememberSaveable { mutableStateOf(RecurrenceFrequency.MONTHLY) }
        var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
        val defaultAccountId = uiState.accounts.firstOrNull()?.id ?: 1L
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddRecurring = false },
            containerColor = ElevatedCharcoal,
            title = { Text("Jadwal Transaksi Berulang", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Transaksi berulang diproses secara lokal saat aplikasi dibuka dengan penanda tanggal terakhir diproses agar bebas duplikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                    GlassTextField(
                        value = title,
                        onValueChange = { title = it; error = null },
                        label = "Keterangan Jadwal",
                        testTag = "recurring_title_input"
                    )
                    GlassTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { ch -> ch.isDigit() }; error = null },
                        label = "Nominal (Rp)",
                        isNumeric = true,
                        testTag = "recurring_amount_input"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TransactionType.INCOME, TransactionType.EXPENSE).forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY, RecurrenceFrequency.MONTHLY, RecurrenceFrequency.YEARLY).forEach { f ->
                            FilterChip(
                                selected = freq == f,
                                onClick = { freq = f },
                                label = { Text(f.label) }
                            )
                        }
                    }
                    if (error != null) Text(error!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                GlassPrimaryButton(
                    text = "Simpan Jadwal",
                    isLoading = uiState.isSaving,
                    onClick = {
                        val amt = amountStr.toLongOrNull() ?: 0L
                        if (title.trim().isEmpty() || amt <= 0L) {
                            error = "Judul dan nominal (> 0) wajib diisi."
                            return@GlassPrimaryButton
                        }
                        onSaveRecurring(
                            RecurringTransactionEntity(
                                title = title.trim(),
                                type = type,
                                amount = amt,
                                accountId = defaultAccountId,
                                frequency = freq,
                                nextRunDate = FinanceCalculator.advanceNextOccurrence(System.currentTimeMillis(), freq)
                            )
                        ) { showAddRecurring = false }
                    },
                    testTag = "save_recurring_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddRecurring = false }) { Text("Batal", color = MutedSilver) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("bills_screen")
    ) {
        GlassTopBar(
            title = "Tagihan & Berulang",
            subtitle = "Pantau tagihan rutin dan jadwal otomatis lokal",
            onBackClick = onBack,
            trailingContent = {
                IconButton(
                    onClick = { if (selectedTab == 0) showAddBill = true else showAddRecurring = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .testTag("add_bill_or_recurring_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = ObsidianBlack)
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Daftar Tagihan (${uiState.bills.size})") }
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Jadwal Berulang (${uiState.recurringTransactions.size})") }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedTab == 0) {
                items(uiState.bills, key = { it.id }) { bill ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bill.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                Text(
                                    "${bill.categoryType.label} • ${bill.frequency.label} • Jatuh tempo: ${FinanceCalculator.formatDate(bill.dueDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSilver
                                )
                                if (bill.lastPaidAt != null) {
                                    Text(
                                        "Terakhir dibayar: ${FinanceCalculator.formatDate(bill.lastPaidAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IncomeGreen
                                    )
                                }
                            }
                            Text(
                                text = FinanceCalculator.formatRupiah(bill.amount, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = if (bill.isPaid) IncomeGreen else WarningAmber
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (!bill.isPaid) {
                                TextButton(
                                    onClick = { onPayBill(bill, uiState.accounts.firstOrNull()?.id) },
                                    modifier = Modifier.testTag("pay_bill_${bill.id}")
                                ) {
                                    Text("Tandai Dibayar", color = IncomeGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(onClick = { onDeleteBill(bill) }) {
                                Text("Hapus", color = ExpenseRed)
                            }
                        }
                    }
                }
            } else {
                items(uiState.recurringTransactions, key = { it.id }) { rec ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                Text(
                                    "${rec.type.label} • ${rec.frequency.label} • Jadwal berikutnya: ${FinanceCalculator.formatDate(rec.nextRunDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSilver
                                )
                            }
                            Text(
                                text = FinanceCalculator.formatRupiah(rec.amount, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = PureWhite
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onToggleRecurring(rec) }) {
                                Text(if (rec.isActive) "Nonaktifkan" else "Aktifkan", color = PureWhite)
                            }
                            TextButton(onClick = { onDeleteRecurring(rec) }) {
                                Text("Hapus", color = ExpenseRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
