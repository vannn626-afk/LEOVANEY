package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DateFilterPeriod
import com.example.data.local.TransactionType
import com.example.domain.FinanceCalculator
import com.example.ui.LeovaneyUiState
import com.example.ui.components.BalanceTrendChart
import com.example.ui.components.CategoryBreakdownList
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.GlassTopBar
import com.example.ui.components.IncomeExpenseComparisonChart
import com.example.ui.components.PeriodFilterRow
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.GlassBorderWhite15
import com.example.ui.theme.GlassBorderWhite28
import com.example.ui.theme.GlassSurfaceWhite12
import com.example.ui.theme.GlassSurfaceWhite8
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.TransferBlueSoft
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: LeovaneyUiState,
    onSelectPeriod: (DateFilterPeriod, Long?, Long?) -> Unit,
    onAddTransactionClick: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val currency = uiState.preferences.currencyCode
    var showCustomDateDialog by rememberSaveable { mutableStateOf(false) }

    if (showCustomDateDialog) {
        CustomDateRangeDialog(
            onDismiss = { showCustomDateDialog = false },
            onApply = { startMillis, endMillis ->
                onSelectPeriod(DateFilterPeriod.CUSTOM, startMillis, endMillis)
                showCustomDateDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassTopBar(
                title = "Selamat datang kembali, ${uiState.preferences.userName}.",
                subtitle = "Ringkasan Keuangan Pribadi • LEOVANEY"
            )
        }

        item {
            PeriodFilterRow(
                selectedPeriod = uiState.selectedDashboardPeriod,
                onSelectPeriod = { period ->
                    if (period == DateFilterPeriod.CUSTOM) {
                        showCustomDateDialog = true
                    } else {
                        onSelectPeriod(period, null, null)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // HERO CARD: Total Saldo Seluruh Rekening & Arus Kas Periode Ini
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("hero_balance_card"),
                borderColor = GlassBorderWhite28
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL SALDO SELURUH REKENING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSilver
                    )
                    Text(
                        text = uiState.selectedDashboardPeriod.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(GlassSurfaceWhite12)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = FinanceCalculator.formatRupiah(uiState.netWorth.activeAccountsTotal, currency),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = PureWhite,
                    modifier = Modifier.testTag("dashboard_total_balance")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricMiniBox(
                        label = "Pemasukan",
                        amount = FinanceCalculator.formatRupiah(uiState.periodIncome, currency),
                        accentColor = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniBox(
                        label = "Pengeluaran",
                        amount = FinanceCalculator.formatRupiah(uiState.periodExpense, currency),
                        accentColor = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurfaceWhite8)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Arus Kas Bersih (${uiState.selectedDashboardPeriod.label})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                    val netColor = when {
                        uiState.periodNetCashFlow > 0L -> IncomeGreen
                        uiState.periodNetCashFlow < 0L -> ExpenseRed
                        else -> PureWhite
                    }
                    Text(
                        text = FinanceCalculator.formatRupiah(
                            uiState.periodNetCashFlow,
                            currency,
                            includePlusSign = true
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = JetBrainsMonoFamily,
                        color = netColor
                    )
                }
            }
        }

        // NET WORTH CARD: Aset, Kewajiban, Kekayaan Bersih
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { onNavigateToRoute("networth") }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KEKAYAAN BERSIH (NET WORTH)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSilver
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FinanceCalculator.formatRupiah(uiState.netWorth.netWorth, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = JetBrainsMonoFamily,
                            color = if (uiState.netWorth.netWorth >= 0L) PureWhite else ExpenseRed
                        )
                    }
                    Text(
                        text = "Kelola Aset →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSilver
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Aset", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                        Text(
                            text = FinanceCalculator.formatRupiah(uiState.netWorth.totalAssets, currency),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFamily,
                            color = IncomeGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Kewajiban", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                        Text(
                            text = FinanceCalculator.formatRupiah(uiState.netWorth.totalLiabilities, currency),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFamily,
                            color = if (uiState.netWorth.totalLiabilities > 0L) ExpenseRed else PureWhite
                        )
                    }
                }
            }
        }

        // QUICK ACCESS NAVIGATION HUB FOR ALL FINANCIAL MODULES
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "AKSES CEPAT FITUR KEUANGAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSilver
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4
                ) {
                    QuickNavTile(
                        title = "Rekening",
                        icon = Icons.Default.AccountBalanceWallet,
                        onClick = { onNavigateToRoute("accounts") },
                        testTag = "quick_nav_accounts",
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavTile(
                        title = "Kategori",
                        icon = Icons.Default.Category,
                        onClick = { onNavigateToRoute("categories") },
                        testTag = "quick_nav_categories",
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavTile(
                        title = "Tabungan",
                        icon = Icons.Default.Savings,
                        onClick = { onNavigateToRoute("savings") },
                        testTag = "quick_nav_savings",
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavTile(
                        title = "Tagihan",
                        icon = Icons.Default.EventRepeat,
                        onClick = { onNavigateToRoute("bills") },
                        testTag = "quick_nav_bills",
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavTile(
                        title = "Utang/Piutang",
                        icon = Icons.Default.Handshake,
                        onClick = { onNavigateToRoute("debts") },
                        testTag = "quick_nav_debts",
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavTile(
                        title = "Aset",
                        icon = Icons.Default.AccountBalance,
                        onClick = { onNavigateToRoute("networth") },
                        testTag = "quick_nav_networth",
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavTile(
                        title = "Backup",
                        icon = Icons.Default.Backup,
                        onClick = { onNavigateToRoute("backup") },
                        testTag = "quick_nav_backup",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // EMPTY STATE IF NO TRANSACTIONS YET
        if (uiState.allTransactions.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("dashboard_empty_state"),
                    borderColor = GlassBorderWhite28
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(GlassSurfaceWhite12),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Transaksi Tercatat",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mulai catat pemasukan, pengeluaran, atau transfer antar-rekening untuk melihat analitik aktual Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSilver
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassPrimaryButton(
                            text = stringResource(id = R.string.add_first_transaction),
                            onClick = onAddTransactionClick,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "add_first_transaction_button"
                        )
                    }
                }
            }
        } else {
            // RECENT TRANSACTIONS
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AKTIVITAS TRANSAKSI TERBARU",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSilver
                        )
                        Text(
                            text = "Lihat Semua →",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite,
                            modifier = Modifier.clickable { onNavigateToRoute("transactions") }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val accountMap = uiState.accounts.associateBy { it.id }
                    val categoryMap = uiState.categories.associateBy { it.id }
                    uiState.allTransactions.take(5).forEach { tx ->
                        val accName = accountMap[tx.accountId]?.name ?: "Rekening"
                        val catName = tx.categoryId?.let { categoryMap[it]?.name }
                            ?: if (tx.type == TransactionType.TRANSFER) "Transfer" else "Lainnya"
                        DashboardTransactionRow(
                            title = tx.title,
                            subtitle = "$catName • $accName • ${FinanceCalculator.formatDate(tx.timestamp, "dd MMM")}",
                            amount = tx.amount,
                            type = tx.type,
                            currencyCode = currency
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // CHARTS: PEMASUKAN & PENGELUARAN + TREN SALDO + KATEGORI
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "GRAFIK PEMASUKAN & PENGELUARAN (6 BULAN)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSilver
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    IncomeExpenseComparisonChart(
                        comparisons = uiState.monthlyComparisons,
                        currencyCode = currency
                    )
                }
            }

            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "TREN SALDO (${uiState.selectedDashboardPeriod.label.uppercase()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSilver
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BalanceTrendChart(points = uiState.balanceTrend)
                }
            }

            if (uiState.expenseByCategory.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "PENGELUARAN BERDASARKAN KATEGORI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSilver
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CategoryBreakdownList(
                            items = uiState.expenseByCategory.take(5),
                            currencyCode = currency,
                            isExpense = true
                        )
                    }
                }
            }
        }

        // BUDGET USAGE SUMMARY
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { onNavigateToRoute("budgets") }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PENGGUNAAN ANGGARAN (${uiState.selectedBudgetYearMonth})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSilver
                    )
                    Text("Atur Anggaran →", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (uiState.budgetSummaries.isEmpty()) {
                    Text(
                        text = "Belum ada batas anggaran bulan ini. Ketuk untuk menetapkan anggaran bulanan atau per kategori.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver
                    )
                } else {
                    uiState.budgetSummaries.take(3).forEach { b ->
                        val statusColor = when {
                            b.isExceeded -> ExpenseRed
                            b.isNearLimit -> WarningAmber
                            else -> IncomeGreen
                        }
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(b.categoryName, style = MaterialTheme.typography.bodyMedium, color = PureWhite)
                                Text(
                                    "${FinanceCalculator.formatRupiah(b.spentAmount, currency)} / ${FinanceCalculator.formatRupiah(b.limitAmount, currency)} (${b.usagePercentage}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor
                                )
                            }
                            if (b.isExceeded || b.isNearLimit) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (b.isExceeded) "Anggaran terlampaui!" else "Mendekati batas anggaran!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SAVINGS GOALS & UPCOMING BILLS SUMMARY
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRoute("savings") }
                ) {
                    Text("TARGET TABUNGAN", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                    Spacer(modifier = Modifier.height(8.dp))
                    val activeGoals = uiState.savingsGoals.filter { !it.isCompleted }
                    Text(
                        text = "${activeGoals.size} Target Aktif",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    val totalSaved = uiState.savingsGoals.sumOf { it.currentAmount }
                    Text(
                        text = "Terkumpul: ${FinanceCalculator.formatRupiah(totalSaved, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IncomeGreen
                    )
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRoute("bills") }
                ) {
                    Text("TAGIHAN MENDATANG", style = MaterialTheme.typography.labelSmall, color = MutedSilver)
                    Spacer(modifier = Modifier.height(8.dp))
                    val unpaidBills = uiState.bills.filter { !it.isPaid }
                    Text(
                        text = "${unpaidBills.size} Belum Dibayar",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    val unpaidTotal = unpaidBills.sumOf { it.amount }
                    Text(
                        text = "Total: ${FinanceCalculator.formatRupiah(unpaidTotal, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unpaidTotal > 0L) WarningAmber else MutedSilver
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    amount: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceWhite8)
            .border(1.dp, GlassBorderWhite15, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MutedSilver)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = JetBrainsMonoFamily,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickNavTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceWhite8)
            .border(1.dp, GlassBorderWhite15, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PureWhite,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DashboardTransactionRow(
    title: String,
    subtitle: String,
    amount: Long,
    type: TransactionType,
    currencyCode: String
) {
    val (icon, tint, bg) = when (type) {
        TransactionType.INCOME -> Triple(Icons.Default.ArrowDownward, IncomeGreen, IncomeGreenSoft)
        TransactionType.EXPENSE -> Triple(Icons.Default.ArrowUpward, ExpenseRed, ExpenseRedSoft)
        TransactionType.TRANSFER -> Triple(Icons.Default.SwapHoriz, TransferBlue, TransferBlueSoft)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceWhite8)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type.label,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        val prefix = when (type) {
            TransactionType.INCOME -> "+"
            TransactionType.EXPENSE -> "-"
            TransactionType.TRANSFER -> "⇄ "
        }
        Text(
            text = "$prefix${FinanceCalculator.formatRupiah(amount, currencyCode)}",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMonoFamily,
            color = tint
        )
    }
}

@Composable
private fun CustomDateRangeDialog(
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    var startStr by rememberSaveable { mutableStateOf(sdf.format(System.currentTimeMillis() - 7L * 86400_000L)) }
    var endStr by rememberSaveable { mutableStateOf(sdf.format(System.currentTimeMillis())) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedCharcoal,
        title = { Text("Rentang Tanggal Khusus", color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Format tanggal: YYYY-MM-DD", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
                GlassTextField(
                    value = startStr,
                    onValueChange = { startStr = it },
                    label = "Tanggal Mulai (YYYY-MM-DD)"
                )
                GlassTextField(
                    value = endStr,
                    onValueChange = { endStr = it },
                    label = "Tanggal Akhir (YYYY-MM-DD)",
                    isError = error != null,
                    supportingText = error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val s = runCatching { sdf.parse(startStr.trim())?.time }.getOrNull()
                    val e = runCatching { sdf.parse(endStr.trim())?.time?.plus(86399_000L) }.getOrNull()
                    if (s == null || e == null) {
                        error = "Format tanggal harus YYYY-MM-DD"
                    } else {
                        onApply(s, e)
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = PureWhite)
            ) {
                Text("Terapkan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = MutedSilver)
            }
        }
    )
}
