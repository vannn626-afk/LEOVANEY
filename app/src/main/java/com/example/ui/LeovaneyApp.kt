package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.LeovaneyLogoBadge
import com.example.ui.components.StatusBanner
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.BillsAndRecurringScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.NetWorthScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SavingsGoalsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.GlassBorderWhite15
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite

object LeovaneyRoutes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val BUDGETS = "budgets"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val SAVINGS = "savings"
    const val BILLS = "bills"
    const val DEBTS = "debts"
    const val NET_WORTH = "networth"
    const val BACKUP = "backup"
}

data class BottomNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val bottomDestinations = listOf(
    BottomNavDestination(
        route = LeovaneyRoutes.DASHBOARD,
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    BottomNavDestination(
        route = LeovaneyRoutes.TRANSACTIONS,
        label = "Transaksi",
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_transactions"
    ),
    BottomNavDestination(
        route = LeovaneyRoutes.BUDGETS,
        label = "Anggaran",
        selectedIcon = Icons.Filled.PieChart,
        unselectedIcon = Icons.Outlined.PieChart,
        testTag = "nav_budgets"
    ),
    BottomNavDestination(
        route = LeovaneyRoutes.REPORTS,
        label = "Laporan",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_reports"
    ),
    BottomNavDestination(
        route = LeovaneyRoutes.SETTINGS,
        label = "Pengaturan",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

@Composable
fun LeovaneyApp(viewModel: LeovaneyViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingBackup by viewModel.pendingBackupPayload.collectAsStateWithLifecycle()

    if (!uiState.isInitialized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack),
            contentAlignment = Alignment.Center
        ) {
            LeovaneyLogoBadge(size = 84.dp)
        }
        return
    }

    if (!uiState.preferences.isOnboardingCompleted) {
        OnboardingScreen(
            initialName = uiState.preferences.userName,
            isSaving = uiState.isSaving,
            onCompleteOnboarding = { name ->
                viewModel.completeOnboarding(name)
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: LeovaneyRoutes.DASHBOARD
    var openTxEditorOnArrival by rememberSaveable { mutableStateOf(false) }

    fun navigateToTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = DeepCharcoal.copy(alpha = 0.96f),
                contentColor = PureWhite,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorderWhite15)
                    .testTag("bottom_navigation_bar")
            ) {
                bottomDestinations.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTopLevel(dest.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.label
                            )
                        },
                        label = {
                            Text(
                                text = dest.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianBlack,
                            selectedTextColor = PureWhite,
                            indicatorColor = PureWhite,
                            unselectedIconColor = MutedSilver,
                            unselectedTextColor = MutedSilver
                        ),
                        modifier = Modifier.testTag(dest.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            StatusBanner(
                statusMessage = uiState.statusMessage,
                errorMessage = uiState.errorMessage,
                onDismiss = { viewModel.clearMessages() }
            )

            NavHost(
                navController = navController,
                startDestination = LeovaneyRoutes.DASHBOARD,
                modifier = Modifier.weight(1f)
            ) {
                composable(LeovaneyRoutes.DASHBOARD) {
                    DashboardScreen(
                        uiState = uiState,
                        onSelectPeriod = { period, start, end ->
                            viewModel.selectDashboardPeriod(period, start, end)
                        },
                        onAddTransactionClick = {
                            openTxEditorOnArrival = true
                            navigateToTopLevel(LeovaneyRoutes.TRANSACTIONS)
                        },
                        onNavigateToRoute = { route ->
                            if (bottomDestinations.any { it.route == route }) {
                                navigateToTopLevel(route)
                            } else {
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                composable(LeovaneyRoutes.TRANSACTIONS) {
                    TransactionsScreen(
                        uiState = uiState,
                        openEditorInitially = openTxEditorOnArrival,
                        onConsumeInitialEditor = { openTxEditorOnArrival = false },
                        onUpdateFilter = { viewModel.updateTransactionFilter(it) },
                        onResetFilter = { viewModel.resetTransactionFilter() },
                        onSaveTransaction = { tx, onDone ->
                            viewModel.saveTransaction(tx, onDone)
                        },
                        onDeleteTransaction = { tx ->
                            viewModel.deleteTransaction(tx)
                        }
                    )
                }

                composable(LeovaneyRoutes.BUDGETS) {
                    BudgetsScreen(
                        uiState = uiState,
                        onNavigateToCategories = {
                            navController.navigate(LeovaneyRoutes.CATEGORIES) {
                                launchSingleTop = true
                            }
                        },
                        onSaveBudget = { b, onDone -> viewModel.saveBudget(b, onDone) },
                        onDeleteBudget = { b -> viewModel.deleteBudget(b) }
                    )
                }

                composable(LeovaneyRoutes.REPORTS) {
                    ReportsScreen(
                        uiState = uiState,
                        onSelectPeriod = { period, start, end ->
                            viewModel.selectDashboardPeriod(period, start, end)
                        },
                        onExportCsv = { cb -> viewModel.generateTransactionsCsv(cb) },
                        onShowMessage = { msg -> viewModel.showStatus(msg) }
                    )
                }

                composable(LeovaneyRoutes.SETTINGS) {
                    SettingsScreen(
                        uiState = uiState,
                        onUpdateProfileName = { newName ->
                            viewModel.updateProfileName(newName)
                        },
                        onNavigateToRoute = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        },
                        onResetAllDataConfirmed = {
                            viewModel.resetAllFinancialData()
                        }
                    )
                }

                composable(LeovaneyRoutes.ACCOUNTS) {
                    AccountsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onSaveAccount = { acc, onDone -> viewModel.saveAccount(acc, onDone) },
                        onToggleArchiveAccount = { acc -> viewModel.toggleArchiveAccount(acc) },
                        onSaveTransfer = { tx, onDone -> viewModel.saveTransaction(tx, onDone) }
                    )
                }

                composable(LeovaneyRoutes.CATEGORIES) {
                    CategoriesScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onSaveCategory = { cat, onDone -> viewModel.saveCategory(cat, onDone) },
                        onToggleArchiveCategory = { cat -> viewModel.toggleArchiveCategory(cat) }
                    )
                }

                composable(LeovaneyRoutes.SAVINGS) {
                    SavingsGoalsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onSaveGoal = { g, onDone -> viewModel.saveSavingsGoal(g, onDone) },
                        onAddContribution = { goalId, amt, isWithdrawal, note, onDone ->
                            viewModel.addSavingsContribution(goalId, amt, isWithdrawal, note, onDone)
                        },
                        onToggleGoalCompleted = { g -> viewModel.toggleSavingsGoalCompleted(g) },
                        onDeleteGoal = { g -> viewModel.deleteSavingsGoal(g) }
                    )
                }

                composable(LeovaneyRoutes.BILLS) {
                    BillsAndRecurringScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onSaveBill = { bill, onDone -> viewModel.saveBill(bill, onDone) },
                        onPayBill = { bill, accId -> viewModel.markBillPaid(bill, accId) },
                        onDeleteBill = { bill -> viewModel.deleteBill(bill) },
                        onSaveRecurring = { rec, onDone -> viewModel.saveRecurringTransaction(rec, onDone) },
                        onToggleRecurring = { rec -> viewModel.toggleRecurringActive(rec) },
                        onDeleteRecurring = { rec -> viewModel.deleteRecurringTransaction(rec) }
                    )
                }

                composable(LeovaneyRoutes.DEBTS) {
                    DebtsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onSaveDebt = { d, onDone -> viewModel.saveDebt(d, onDone) },
                        onRecordPayment = { id, p, fee, accId, note, onDone ->
                            viewModel.recordDebtPayment(id, p, fee, accId, note, onDone)
                        },
                        onDeleteDebt = { d -> viewModel.deleteDebt(d) }
                    )
                }

                composable(LeovaneyRoutes.NET_WORTH) {
                    NetWorthScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onSaveAsset = { a, onDone -> viewModel.saveAsset(a, onDone) },
                        onDeleteAsset = { a -> viewModel.deleteAsset(a) },
                        onSaveLiability = { l, onDone -> viewModel.saveLiability(l, onDone) },
                        onDeleteLiability = { l -> viewModel.deleteLiability(l) }
                    )
                }

                composable(LeovaneyRoutes.BACKUP) {
                    BackupScreen(
                        uiState = uiState,
                        pendingBackupPayload = pendingBackup,
                        onBack = { navController.popBackStack() },
                        onGenerateBackupJson = { cb -> viewModel.generateBackupJson(cb) },
                        onGenerateCsv = { cb -> viewModel.generateTransactionsCsv(cb) },
                        onStageBackupJson = { raw -> viewModel.stageBackupForConfirmation(raw) },
                        onCancelStagedBackup = { viewModel.cancelStagedBackup() },
                        onConfirmRestore = { mode -> viewModel.confirmRestoreBackup(mode) },
                        onShowMessage = { msg -> viewModel.showStatus(msg) }
                    )
                }
            }
        }
    }
}
