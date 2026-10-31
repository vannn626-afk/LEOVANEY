package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.data.local.AssetEntity
import com.example.data.local.AssetType
import com.example.data.local.DebtEntity
import com.example.data.local.DebtType
import com.example.data.local.LiabilityEntity
import com.example.data.local.LiabilityType
import com.example.domain.FinanceCalculator
import com.example.ui.LeovaneyUiState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.GlassTopBar
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
fun DebtsScreen(
    uiState: LeovaneyUiState,
    onBack: () -> Unit,
    onSaveDebt: (DebtEntity, () -> Unit) -> Unit,
    onRecordPayment: (Long, Long, Long, Long?, String, () -> Unit) -> Unit,
    onDeleteDebt: (DebtEntity) -> Unit
) {
    BackHandler(onBack = onBack)
    val currency = uiState.preferences.currencyCode
    var showAddDebt by rememberSaveable { mutableStateOf(false) }
    var paymentDebtId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (showAddDebt) {
        var type by rememberSaveable { mutableStateOf(DebtType.DEBT) }
        var personName by rememberSaveable { mutableStateOf("") }
        var principalStr by rememberSaveable { mutableStateOf("") }
        var feeStr by rememberSaveable { mutableStateOf("0") }
        var note by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDebt = false },
            containerColor = ElevatedCharcoal,
            title = { Text("Catat Utang / Piutang", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DebtType.entries.forEach { dt ->
                            FilterChip(
                                selected = type == dt,
                                onClick = { type = dt },
                                label = { Text(dt.label) }
                            )
                        }
                    }
                    GlassTextField(
                        value = personName,
                        onValueChange = { personName = it; error = null },
                        label = "Nama Pihak / Pemberi / Peminjam",
                        testTag = "debt_person_input"
                    )
                    GlassTextField(
                        value = principalStr,
                        onValueChange = { principalStr = it.filter { ch -> ch.isDigit() }; error = null },
                        label = "Nominal Pokok (Rp)",
                        isNumeric = true,
                        testTag = "debt_principal_input"
                    )
                    GlassTextField(
                        value = feeStr,
                        onValueChange = { feeStr = it.filter { ch -> ch.isDigit() } },
                        label = "Estimasi Bunga / Biaya (Opsional, Rp)",
                        isNumeric = true
                    )
                    GlassTextField(value = note, onValueChange = { note = it }, label = "Catatan")
                    if (error != null) Text(error!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                GlassPrimaryButton(
                    text = "Simpan",
                    isLoading = uiState.isSaving,
                    onClick = {
                        val principal = principalStr.toLongOrNull() ?: 0L
                        if (personName.trim().isEmpty() || principal <= 0L) {
                            error = "Nama pihak dan nominal pokok (> 0) wajib diisi."
                            return@GlassPrimaryButton
                        }
                        onSaveDebt(
                            DebtEntity(
                                type = type,
                                personName = personName.trim(),
                                principalAmount = principal,
                                interestOrFeeAmount = feeStr.toLongOrNull() ?: 0L,
                                remainingPrincipal = principal,
                                note = note.trim()
                            )
                        ) { showAddDebt = false }
                    },
                    testTag = "save_debt_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddDebt = false }) { Text("Batal", color = MutedSilver) }
            }
        )
    }

    if (paymentDebtId != null) {
        val debt = uiState.debts.find { it.id == paymentDebtId }
        if (debt != null) {
            var principalPaidStr by rememberSaveable { mutableStateOf(debt.remainingPrincipal.toString()) }
            var interestPaidStr by rememberSaveable { mutableStateOf("0") }
            var note by rememberSaveable { mutableStateOf("") }
            var error by rememberSaveable { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { paymentDebtId = null },
                containerColor = ElevatedCharcoal,
                title = { Text("Pembayaran ${debt.type.label}: ${debt.personName}", color = PureWhite) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Pokok dipisahkan dari bunga/biaya agar pelunasan pokok tidak dihitung dua kali sebagai pengeluaran baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSilver
                        )
                        GlassTextField(
                            value = principalPaidStr,
                            onValueChange = { principalPaidStr = it.filter { ch -> ch.isDigit() }; error = null },
                            label = "Pembayaran Pokok (Rp)",
                            isNumeric = true,
                            testTag = "debt_payment_principal_input"
                        )
                        GlassTextField(
                            value = interestPaidStr,
                            onValueChange = { interestPaidStr = it.filter { ch -> ch.isDigit() } },
                            label = "Bunga / Biaya Tambahan (Rp)",
                            isNumeric = true
                        )
                        GlassTextField(value = note, onValueChange = { note = it }, label = "Catatan Pembayaran")
                        if (error != null) Text(error!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    GlassPrimaryButton(
                        text = "Catat Pembayaran",
                        isLoading = uiState.isSaving,
                        onClick = {
                            val p = principalPaidStr.toLongOrNull() ?: 0L
                            val fee = interestPaidStr.toLongOrNull() ?: 0L
                            if (p <= 0L && fee <= 0L) {
                                error = "Masukkan nominal pembayaran (> 0)."
                                return@GlassPrimaryButton
                            }
                            onRecordPayment(
                                debt.id,
                                p,
                                fee,
                                uiState.accounts.firstOrNull()?.id,
                                note.trim()
                            ) { paymentDebtId = null }
                        },
                        testTag = "confirm_debt_payment_button"
                    )
                },
                dismissButton = {
                    TextButton(onClick = { paymentDebtId = null }) { Text("Batal", color = MutedSilver) }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("debts_screen")
    ) {
        GlassTopBar(
            title = "Utang & Piutang",
            subtitle = "Pisahkan pokok dan bunga secara konsisten",
            onBackClick = onBack,
            trailingContent = {
                IconButton(
                    onClick = { showAddDebt = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .testTag("add_debt_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Utang/Piutang", tint = ObsidianBlack)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text("SISA UTANG", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FinanceCalculator.formatRupiah(uiState.netWorth.activeDebtsTotal, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFamily,
                            color = ExpenseRed
                        )
                    }
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text("SISA PIUTANG", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FinanceCalculator.formatRupiah(uiState.netWorth.activeReceivablesTotal, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFamily,
                            color = IncomeGreen
                        )
                    }
                }
            }

            items(uiState.debts, key = { it.id }) { debt ->
                val payments = uiState.debtPayments.filter { it.debtId == debt.id }
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${debt.type.label}: ${debt.personName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            Text(
                                text = "Pokok Awal: ${FinanceCalculator.formatRupiah(debt.principalAmount, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSilver
                            )
                        }
                        Text(
                            text = if (debt.isSettled) "LUNAS" else FinanceCalculator.formatRupiah(debt.remainingPrincipal, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFamily,
                            color = if (debt.isSettled) IncomeGreen else WarningAmber
                        )
                    }
                    if (payments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        payments.take(3).forEach { p ->
                            Text(
                                "• Bayar pokok ${FinanceCalculator.formatRupiah(p.principalPaid, currency)} (${FinanceCalculator.formatDate(p.timestamp, "dd MMM")})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSilver
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!debt.isSettled) {
                            TextButton(
                                onClick = { paymentDebtId = debt.id },
                                modifier = Modifier.testTag("pay_debt_btn_${debt.id}")
                            ) {
                                Text("Catat Pembayaran / Pelunasan", color = PureWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { onDeleteDebt(debt) }) {
                            Text("Hapus", color = ExpenseRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthScreen(
    uiState: LeovaneyUiState,
    onBack: () -> Unit,
    onSaveAsset: (AssetEntity, () -> Unit) -> Unit,
    onDeleteAsset: (AssetEntity) -> Unit,
    onSaveLiability: (LiabilityEntity, () -> Unit) -> Unit,
    onDeleteLiability: (LiabilityEntity) -> Unit
) {
    BackHandler(onBack = onBack)
    val currency = uiState.preferences.currencyCode
    var showAddAsset by rememberSaveable { mutableStateOf(false) }
    var showAddLiability by rememberSaveable { mutableStateOf(false) }

    if (showAddAsset) {
        var name by rememberSaveable { mutableStateOf("") }
        var type by rememberSaveable { mutableStateOf(AssetType.INVESTASI_MANUAL) }
        var valueStr by rememberSaveable { mutableStateOf("") }
        var note by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddAsset = false },
            containerColor = ElevatedCharcoal,
            title = { Text("Tambah Aset Manual", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Jangan memasukkan saldo rekening yang sudah terdaftar di menu Rekening agar tidak dihitung dua kali.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = "Nama Aset",
                        testTag = "asset_name_input"
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssetType.entries.forEach { at ->
                            FilterChip(
                                selected = type == at,
                                onClick = { type = at },
                                label = { Text(at.label) }
                            )
                        }
                    }
                    GlassTextField(
                        value = valueStr,
                        onValueChange = { valueStr = it.filter { ch -> ch.isDigit() }; error = null },
                        label = "Nilai Aset Saat Ini (Rp)",
                        isNumeric = true,
                        testTag = "asset_value_input"
                    )
                    GlassTextField(value = note, onValueChange = { note = it }, label = "Catatan")
                    if (error != null) Text(error!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                GlassPrimaryButton(
                    text = "Simpan Aset",
                    isLoading = uiState.isSaving,
                    onClick = {
                        val v = valueStr.toLongOrNull() ?: -1L
                        if (name.trim().isEmpty() || v < 0L) {
                            error = "Nama aset dan nilai valid wajib diisi."
                            return@GlassPrimaryButton
                        }
                        onSaveAsset(
                            AssetEntity(
                                name = name.trim(),
                                type = type,
                                value = v,
                                note = note.trim()
                            )
                        ) { showAddAsset = false }
                    },
                    testTag = "save_asset_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddAsset = false }) { Text("Batal", color = MutedSilver) }
            }
        )
    }

    if (showAddLiability) {
        var name by rememberSaveable { mutableStateOf("") }
        var type by rememberSaveable { mutableStateOf(LiabilityType.PINJAMAN) }
        var amountStr by rememberSaveable { mutableStateOf("") }
        var note by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddLiability = false },
            containerColor = ElevatedCharcoal,
            title = { Text("Tambah Kewajiban", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = "Nama Kewajiban / Cicilan",
                        testTag = "liability_name_input"
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiabilityType.entries.forEach { lt ->
                            FilterChip(
                                selected = type == lt,
                                onClick = { type = lt },
                                label = { Text(lt.label) }
                            )
                        }
                    }
                    GlassTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { ch -> ch.isDigit() }; error = null },
                        label = "Nominal Kewajiban (Rp)",
                        isNumeric = true,
                        testTag = "liability_amount_input"
                    )
                    GlassTextField(value = note, onValueChange = { note = it }, label = "Catatan")
                    if (error != null) Text(error!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                GlassPrimaryButton(
                    text = "Simpan Kewajiban",
                    isLoading = uiState.isSaving,
                    onClick = {
                        val amt = amountStr.toLongOrNull() ?: -1L
                        if (name.trim().isEmpty() || amt < 0L) {
                            error = "Nama dan nominal kewajiban wajib diisi."
                            return@GlassPrimaryButton
                        }
                        onSaveLiability(
                            LiabilityEntity(
                                name = name.trim(),
                                type = type,
                                amount = amt,
                                note = note.trim()
                            )
                        ) { showAddLiability = false }
                    },
                    testTag = "save_liability_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddLiability = false }) { Text("Batal", color = MutedSilver) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("networth_screen")
    ) {
        GlassTopBar(
            title = "Aset & Kewajiban",
            subtitle = "Kalkulasi Kekayaan Bersih tanpa duplikasi saldo",
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val nw = uiState.netWorth
                GlassCard(borderColor = GlassBorderWhite28) {
                    Text("KEKAYAAN BERSIH (TOTAL ASET - TOTAL KEWAJIBAN)", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FinanceCalculator.formatRupiah(nw.netWorth, currency),
                        style = MaterialTheme.typography.displayMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = if (nw.netWorth >= 0L) PureWhite else ExpenseRed,
                        modifier = Modifier.testTag("net_worth_total_value")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Saldo Rekening Aktif: ${FinanceCalculator.formatRupiah(nw.activeAccountsTotal, currency)}", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                    Text("• Aset Manual (Properti/Kendaraan/Investasi): ${FinanceCalculator.formatRupiah(nw.manualAssetsTotal, currency)}", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                    Text("• Piutang Aktif: ${FinanceCalculator.formatRupiah(nw.activeReceivablesTotal, currency)}", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                    Text("= Total Aset: ${FinanceCalculator.formatRupiah(nw.totalAssets, currency)}", style = MaterialTheme.typography.labelMedium, color = IncomeGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Kewajiban Manual: ${FinanceCalculator.formatRupiah(nw.manualLiabilitiesTotal, currency)}", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                    Text("• Utang Aktif: ${FinanceCalculator.formatRupiah(nw.activeDebtsTotal, currency)}", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                    Text("= Total Kewajiban: ${FinanceCalculator.formatRupiah(nw.totalLiabilities, currency)}", style = MaterialTheme.typography.labelMedium, color = ExpenseRed)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassPrimaryButton(
                        text = "+ Aset Manual",
                        onClick = { showAddAsset = true },
                        modifier = Modifier.weight(1f),
                        testTag = "open_add_asset_button"
                    )
                    GlassPrimaryButton(
                        text = "+ Kewajiban",
                        onClick = { showAddLiability = true },
                        modifier = Modifier.weight(1f),
                        testTag = "open_add_liability_button"
                    )
                }
            }

            item {
                Text("DAFTAR ASET MANUAL (${uiState.assets.size})", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
            }
            items(uiState.assets, key = { "asset_${it.id}" }) { asset ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(asset.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                            Text(asset.type.label, style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                        }
                        Text(
                            text = FinanceCalculator.formatRupiah(asset.value, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFamily,
                            color = IncomeGreen
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onDeleteAsset(asset) }) {
                            Text("Hapus", color = ExpenseRed)
                        }
                    }
                }
            }

            item {
                Text("DAFTAR KEWAJIBAN MANUAL (${uiState.liabilities.size})", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
            }
            items(uiState.liabilities, key = { "liab_${it.id}" }) { liab ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(liab.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                            Text(liab.type.label, style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                        }
                        Text(
                            text = FinanceCalculator.formatRupiah(liab.amount, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFamily,
                            color = ExpenseRed
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onDeleteLiability(liab) }) {
                            Text("Hapus", color = ExpenseRed)
                        }
                    }
                }
            }
        }
    }
}
