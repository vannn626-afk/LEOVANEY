package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupPayload
import com.example.data.backup.RestoreMode
import com.example.data.local.DateFilterPeriod
import com.example.domain.FinanceCalculator
import com.example.ui.LeovaneyUiState
import com.example.ui.components.BalanceTrendChart
import com.example.ui.components.CategoryBreakdownList
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.GlassTopBar
import com.example.ui.components.IncomeExpenseComparisonChart
import com.example.ui.components.LeovaneyLogoBadge
import com.example.ui.components.PeriodFilterRow
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassBorderWhite28
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningAmber

@Composable
fun ReportsScreen(
    uiState: LeovaneyUiState,
    onSelectPeriod: (DateFilterPeriod, Long?, Long?) -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val currency = uiState.preferences.currencyCode
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }

    val csvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = pendingCsvContent
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                onShowMessage("Laporan CSV berhasil diekspor ke perangkat.")
            }
        }
        pendingCsvContent = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("reports_screen"),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassTopBar(
                title = "Laporan Keuangan Aktual",
                subtitle = "Dihitung langsung dari database lokal",
                trailingContent = {
                    IconButton(
                        onClick = {
                            onExportCsv { csv ->
                                pendingCsvContent = csv
                                csvDocumentLauncher.launch("leovaney_transaksi_${System.currentTimeMillis()}.csv")
                            }
                        },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Ekspor CSV", tint = PureWhite)
                    }
                }
            )
        }

        item {
            PeriodFilterRow(
                selectedPeriod = uiState.selectedDashboardPeriod,
                onSelectPeriod = { onSelectPeriod(it, null, null) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 1. Pemasukan, Pengeluaran, Arus Kas
        item {
            GlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                borderColor = GlassBorderWhite28
            ) {
                Text(
                    "RINGKASAN ARUS KAS (${uiState.selectedDashboardPeriod.label.uppercase()})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSilver
                )
                Spacer(modifier = Modifier.height(10.dp))
                ReportMetricRow("Total Pemasukan", FinanceCalculator.formatRupiah(uiState.periodIncome, currency), IncomeGreen)
                ReportMetricRow("Total Pengeluaran", FinanceCalculator.formatRupiah(uiState.periodExpense, currency), ExpenseRed)
                ReportMetricRow(
                    "Arus Kas Bersih",
                    FinanceCalculator.formatRupiah(uiState.periodNetCashFlow, currency, includePlusSign = true),
                    if (uiState.periodNetCashFlow >= 0L) IncomeGreen else ExpenseRed
                )
            }
        }

        // 2. Perbandingan Antarperiode & Tren Bulanan
        item {
            GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("PERBANDINGAN ANTARPERIODE (6 BULAN)", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Spacer(modifier = Modifier.height(12.dp))
                IncomeExpenseComparisonChart(comparisons = uiState.monthlyComparisons, currencyCode = currency)
            }
        }

        // 3. Tren Saldo
        item {
            GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("TREN SALDO AKTUAL", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Spacer(modifier = Modifier.height(12.dp))
                BalanceTrendChart(points = uiState.balanceTrend)
            }
        }

        // 4. Pengeluaran & Pemasukan per Kategori
        item {
            GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("PENGELUARAN PER KATEGORI", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Spacer(modifier = Modifier.height(10.dp))
                if (uiState.expenseByCategory.isEmpty()) {
                    Text("Belum ada pengeluaran pada periode ini.", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                } else {
                    CategoryBreakdownList(items = uiState.expenseByCategory, currencyCode = currency, isExpense = true)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("PEMASUKAN PER KATEGORI", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Spacer(modifier = Modifier.height(10.dp))
                if (uiState.incomeByCategory.isEmpty()) {
                    Text("Belum ada pemasukan pada periode ini.", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                } else {
                    CategoryBreakdownList(items = uiState.incomeByCategory, currencyCode = currency, isExpense = false)
                }
            }
        }

        // 5. Saldo per Rekening
        item {
            GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("SALDO PER REKENING", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Spacer(modifier = Modifier.height(10.dp))
                uiState.accountBalances.forEach { acc ->
                    ReportMetricRow(
                        "${acc.account.name} (${acc.account.type.label})",
                        FinanceCalculator.formatRupiah(acc.currentBalance, currency),
                        PureWhite
                    )
                }
            }
        }

        // 6. Laporan Kekayaan Bersih, Aset, Kewajiban, Utang/Piutang, Target Tabungan
        item {
            val nw = uiState.netWorth
            GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("LAPORAN KEKAYAAN BERSIH, UTANG & TABUNGAN", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Spacer(modifier = Modifier.height(10.dp))
                ReportMetricRow("Total Aset", FinanceCalculator.formatRupiah(nw.totalAssets, currency), IncomeGreen)
                ReportMetricRow("Total Kewajiban", FinanceCalculator.formatRupiah(nw.totalLiabilities, currency), ExpenseRed)
                ReportMetricRow("Kekayaan Bersih", FinanceCalculator.formatRupiah(nw.netWorth, currency), PureWhite)
                ReportMetricRow("Sisa Utang Belum Lunas", FinanceCalculator.formatRupiah(nw.activeDebtsTotal, currency), WarningAmber)
                ReportMetricRow("Sisa Piutang Belum Lunas", FinanceCalculator.formatRupiah(nw.activeReceivablesTotal, currency), IncomeGreen)
                val totalSaved = uiState.savingsGoals.sumOf { it.currentAmount }
                ReportMetricRow("Total Terkumpul di Target Tabungan", FinanceCalculator.formatRupiah(totalSaved, currency), IncomeGreen)
            }
        }
    }
}

@Composable
private fun ReportMetricRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MutedSilver)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMonoFamily,
            color = valueColor
        )
    }
}

@Composable
fun BackupScreen(
    uiState: LeovaneyUiState,
    pendingBackupPayload: BackupPayload?,
    onBack: () -> Unit,
    onGenerateBackupJson: ((String) -> Unit) -> Unit,
    onGenerateCsv: ((String) -> Unit) -> Unit,
    onStageBackupJson: (String) -> Unit,
    onCancelStagedBackup: () -> Unit,
    onConfirmRestore: (RestoreMode) -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingExportCsv by remember { mutableStateOf<String?>(null) }
    var manualJsonInput by rememberSaveable { mutableStateOf("") }
    var showManualJsonBox by rememberSaveable { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                onShowMessage("File backup JSON berhasil disimpan ke perangkat.")
            }
        }
        pendingExportJson = null
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }.onSuccess { content ->
                onStageBackupJson(content)
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val csv = pendingExportCsv
        if (uri != null && csv != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                onShowMessage("File CSV berhasil disimpan ke perangkat.")
            }
        }
        pendingExportCsv = null
    }

    if (pendingBackupPayload != null) {
        RestoreConfirmationDialog(
            payload = pendingBackupPayload,
            onConfirm = onConfirmRestore,
            onDismiss = onCancelStagedBackup
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("backup_screen")
    ) {
        GlassTopBar(
            title = "Backup & Pemulihan Data",
            subtitle = "Storage Access Framework • JSON & CSV",
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(borderColor = GlassBorderWhite28) {
                    Text("CADANGKAN SELURUH DATABASE (JSON)", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "File backup mencakup seluruh rekening, transaksi, kategori, anggaran, target tabungan, tagihan, jadwal berulang, utang/piutang, aset/kewajiban, versi format, dan profil pengguna.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassPrimaryButton(
                        text = "Ekspor Backup JSON ke Perangkat",
                        onClick = {
                            onGenerateBackupJson { json ->
                                pendingExportJson = json
                                manualJsonInput = json
                                createBackupLauncher.launch("leovaney_backup_${System.currentTimeMillis()}.json")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "export_backup_json_button"
                    )
                }
            }

            item {
                GlassCard {
                    Text("PULIHKAN DARI FILE BACKUP (JSON)", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Sebelum pemulihan dijalankan, aplikasi akan memvalidasi keutuhan file, memeriksa versi format, dan meminta konfirmasi apakah data akan diganti atau digabungkan di dalam transaksi database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassPrimaryButton(
                        text = "Pilih File Backup JSON (.json)",
                        onClick = { openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "import_backup_file_button"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showManualJsonBox = !showManualJsonBox },
                        modifier = Modifier.testTag("toggle_manual_json_button")
                    ) {
                        Text(
                            if (showManualJsonBox) "Sembunyikan Input JSON Manual" else "Salin / Tempel JSON Backup Secara Langsung",
                            color = PureWhite
                        )
                    }
                    if (showManualJsonBox) {
                        GlassTextField(
                            value = manualJsonInput,
                            onValueChange = { manualJsonInput = it },
                            label = "Isi JSON Backup",
                            singleLine = false,
                            testTag = "manual_backup_json_input"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GlassPrimaryButton(
                            text = "Validasi & Pulihkan JSON Ini",
                            onClick = { onStageBackupJson(manualJsonInput) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "validate_manual_json_button"
                        )
                    }
                }
            }

            item {
                GlassCard {
                    Text("EKSPOR LAPORAN TRANSAKSI (CSV)", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassPrimaryButton(
                        text = "Ekspor Transaksi ke CSV",
                        onClick = {
                            onGenerateCsv { csv ->
                                pendingExportCsv = csv
                                createCsvLauncher.launch("leovaney_transaksi_${System.currentTimeMillis()}.csv")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "backup_export_csv_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun RestoreConfirmationDialog(
    payload: BackupPayload,
    onConfirm: (RestoreMode) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by rememberSaveable { mutableStateOf(RestoreMode.REPLACE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedCharcoal,
        title = { Text("Konfirmasi Pemulihan Backup", color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "File backup valid (Versi ${payload.backupVersion}) milik profil \"${payload.userName}\".",
                    color = IncomeGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Ringkasan isi backup:\n" +
                        "• ${payload.accounts.size} Rekening & ${payload.categories.size} Kategori\n" +
                        "• ${payload.transactions.size} Transaksi & ${payload.budgets.size} Anggaran\n" +
                        "• ${payload.savingsGoals.size} Target Tabungan & ${payload.bills.size} Tagihan\n" +
                        "• ${payload.debts.size} Utang/Piutang & ${payload.assets.size} Aset",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSilver
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("Pilih Metode Pemulihan:", style = MaterialTheme.typography.labelSmall, color = PureWhite)
                RestoreMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("restore_mode_${mode.name}")
                    )
                    Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                }
            }
        },
        confirmButton = {
            GlassPrimaryButton(
                text = "Pulihkan Sekarang",
                onClick = { onConfirm(selectedMode) },
                testTag = "confirm_restore_backup_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = MutedSilver) }
        }
    )
}

@Composable
fun SettingsScreen(
    uiState: LeovaneyUiState,
    onUpdateProfileName: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onResetAllDataConfirmed: () -> Unit
) {
    var nameInput by rememberSaveable(uiState.preferences.userName) {
        mutableStateOf(uiState.preferences.userName)
    }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var showResetConfirmDialog by rememberSaveable { mutableStateOf(false) }

    if (showResetConfirmDialog) {
        ConfirmDeleteDialog(
            title = "Reset Seluruh Data Keuangan?",
            message = "Tindakan ini akan menghapus seluruh transaksi, anggaran, target tabungan, tagihan, utang/piutang, dan aset dari perangkat ini. Nama profil Anda (${uiState.preferences.userName}) tetap dipertahankan. Pastikan Anda sudah membuat backup JSON terlebih dahulu.",
            confirmText = "Ya, Reset Data",
            onConfirm = onResetAllDataConfirmed,
            onDismiss = { showResetConfirmDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassTopBar(
                title = "Pengaturan & Profil",
                subtitle = "Identitas pengguna tunggal & privasi offline"
            )
        }

        // PROFILE NAME SETTINGS
        item {
            GlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                borderColor = GlassBorderWhite28
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LeovaneyLogoBadge(size = 48.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("PROFIL PENGGUNA", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                        Text(
                            text = uiState.preferences.userName,
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite,
                            modifier = Modifier.testTag("settings_current_name")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                GlassTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        nameError = null
                    },
                    label = "Ubah Nama Panggilan",
                    isError = nameError != null,
                    supportingText = nameError ?: "Mengubah nama akan langsung memperbarui sapaan dashboard tanpa menghapus data keuangan atau mengulang onboarding.",
                    testTag = "settings_name_input"
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassPrimaryButton(
                    text = "Simpan Perubahan Nama",
                    isLoading = uiState.isSaving,
                    onClick = {
                        val trimmed = nameInput.trim()
                        if (trimmed.isEmpty()) {
                            nameError = "Nama tidak boleh kosong."
                        } else {
                            onUpdateProfileName(trimmed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_profile_name_button"
                )
            }
        }

        // QUICK LINKS TO ALL SUB-MODULES
        item {
            GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("MANAJEMEN MODUL KEUANGAN", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsNavigationItem("Rekening & Dompet", "Tunai, Bank, E-Wallet, Tabungan, Transfer") {
                    onNavigateToRoute("accounts")
                }
                SettingsNavigationItem("Kategori Pemasukan & Pengeluaran", "Buat, edit, atau arsipkan kategori") {
                    onNavigateToRoute("categories")
                }
                SettingsNavigationItem("Target Tabungan", "Pantau dana impian & riwayat kontribusi") {
                    onNavigateToRoute("savings")
                }
                SettingsNavigationItem("Tagihan & Transaksi Berulang", "Listrik, internet, sewa, cicilan & jadwal lokal") {
                    onNavigateToRoute("bills")
                }
                SettingsNavigationItem("Utang & Piutang", "Catatan pokok, bunga/biaya, dan pelunasan") {
                    onNavigateToRoute("debts")
                }
                SettingsNavigationItem("Aset & Kewajiban (Kekayaan Bersih)", "Properti, kendaraan, investasi manual & pinjaman") {
                    onNavigateToRoute("networth")
                }
                SettingsNavigationItem("Backup & Pemulihan Data (JSON / CSV)", "Ekspor dan impor database lokal via Android SAF") {
                    onNavigateToRoute("backup")
                }
            }
        }

        // PRIVACY & OFFLINE NOTICE
        item {
            GlassCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("PRIVASI & PENYIMPANAN LOKAL OFFLINE", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Seluruh data keuangan inti tersimpan 100% secara lokal di perangkat ini menggunakan Room Database dan DataStore Preferences.\n" +
                        "• LEOVANEY berjalan sepenuhnya offline tanpa login, tanpa password, tanpa server eksternal, tanpa pelacakan tersembunyi, dan tanpa AI agent.\n" +
                        "• Perhatian: Data lokal dapat hilang apabila aplikasi dihapus (uninstall), data aplikasi dibersihkan, atau perangkat rusak. Buatlah backup JSON secara berkala melalui menu Backup & Pemulihan.\n" +
                        "• Karena aplikasi dirancang tanpa login untuk kemudahan akses satu pengguna, pastikan perangkat Anda menggunakan kunci layar sistem Android agar tidak diakses pihak lain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSilver
                )
            }
        }

        // DANGER ZONE: RESET DATA WITH CONFIRMATION
        item {
            GlassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                borderColor = ExpenseRed.copy(alpha = 0.5f)
            ) {
                Text("ZONA BERBAHAYA", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Menghapus seluruh catatan transaksi dan mengembalikan database keuangan ke kondisi awal. Memerlukan konfirmasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSilver
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_reset_data_dialog_button")
                ) {
                    Text("Reset Seluruh Data Keuangan", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = PureWhite, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedSilver)
            }
            Text("→", style = MaterialTheme.typography.titleMedium, color = PureWhite)
        }
    }
}
