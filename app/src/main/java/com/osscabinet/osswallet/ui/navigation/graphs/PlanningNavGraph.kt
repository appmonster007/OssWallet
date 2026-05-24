package com.osscabinet.ossWallet.ui.navigation.graphs

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.osscabinet.ossWallet.ui.navigation.BudgetDetailRoute
import com.osscabinet.ossWallet.ui.navigation.BudgetEntryRoute
import com.osscabinet.ossWallet.ui.navigation.BudgetListRoute
import com.osscabinet.ossWallet.ui.navigation.EnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.ExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.LoanDetailRoute
import com.osscabinet.ossWallet.ui.navigation.LoanEntryRoute
import com.osscabinet.ossWallet.ui.navigation.LoanListRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentDetailRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentEntryRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentListRoute
import com.osscabinet.ossWallet.ui.navigation.PopEnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.PopExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.navigation.TransactionEntryRoute
import com.osscabinet.ossWallet.ui.navigation.navigateToRecords
import com.osscabinet.ossWallet.ui.screens.budgets.BudgetViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.detail.BudgetDetailScreen
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryScreen
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.list.BudgetListScreen
import com.osscabinet.ossWallet.ui.screens.loans.LoanViewModel
import com.osscabinet.ossWallet.ui.screens.loans.detail.LoanDetailScreen
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryScreen
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryViewModel
import com.osscabinet.ossWallet.ui.screens.loans.list.LoanListScreen
import com.osscabinet.ossWallet.ui.screens.plannedpayments.PlannedPaymentViewModel
import com.osscabinet.ossWallet.ui.screens.plannedpayments.detail.PlannedPaymentDetailScreen
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryScreen
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryViewModel
import com.osscabinet.ossWallet.ui.screens.plannedpayments.list.PlannedPaymentListScreen
import androidx.compose.runtime.getValue

fun NavGraphBuilder.planningGraph(
    navController: NavHostController,
    vms: SharedNavViewModels
) {
    // ── Planned Payments ──────────────────────────────────────────────────────

    composable<PlannedPaymentListRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        val plannedPaymentViewModel: PlannedPaymentViewModel = hiltViewModel()
        PlannedPaymentListScreen(
            viewModel = plannedPaymentViewModel,
            transactionManager = vms.dashboardViewModel.transactionManager,
            onAddClick = { navController.navigate(PlannedPaymentEntryRoute()) },
            onEditClick = { id -> navController.navigate(PlannedPaymentEntryRoute(plannedPaymentId = id)) },
            onDetailClick = { id -> navController.navigate(PlannedPaymentDetailRoute(id)) },
            onConfirmClick = { id, date ->
                vms.transactionViewModel.setPlannedPaymentPrefill(id, date)
                navController.navigate(TransactionEntryRoute())
            },
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<PlannedPaymentEntryRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<PlannedPaymentEntryRoute>()
        val entryViewModel: PlannedPaymentEntryViewModel = hiltViewModel()
        PlannedPaymentEntryScreen(
            plannedPaymentId = route.plannedPaymentId.takeIf { it != -1L },
            viewModel = entryViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<PlannedPaymentDetailRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<PlannedPaymentDetailRoute>()
        val plannedPaymentViewModel: PlannedPaymentViewModel = hiltViewModel()
        val accounts by vms.accountViewModel.accounts.collectAsStateWithLifecycle()
        val categories by vms.transactionViewModel.categories.collectAsStateWithLifecycle()
        PlannedPaymentDetailScreen(
            plannedPaymentId = route.plannedPaymentId,
            viewModel = plannedPaymentViewModel,
            transactionManager = vms.dashboardViewModel.transactionManager,
            accounts = accounts,
            categories = categories,
            onEditClick = { id -> navController.navigate(PlannedPaymentEntryRoute(plannedPaymentId = id)) },
            onConfirmClick = { id, date ->
                vms.transactionViewModel.setPlannedPaymentPrefill(id, date)
                navController.navigate(TransactionEntryRoute())
            },
            onBackClick = { navController.popBackStack() }
        )
    }

    // ── Budgets ───────────────────────────────────────────────────────────────

    composable<BudgetListRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        val budgetViewModel: BudgetViewModel = hiltViewModel()
        val baseCurrency by vms.settingsViewModel.currency.collectAsStateWithLifecycle()
        BudgetListScreen(
            viewModel = budgetViewModel,
            onAddClick = { navController.navigate(BudgetEntryRoute()) },
            onEditClick = { id -> navController.navigate(BudgetEntryRoute(budgetId = id)) },
            onDetailClick = { id -> navController.navigate(BudgetDetailRoute(id)) },
            onBackClick = { navController.popBackStack() },
            baseCurrency = baseCurrency
        )
    }

    composable<BudgetEntryRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<BudgetEntryRoute>()
        val entryViewModel: BudgetEntryViewModel = hiltViewModel()
        BudgetEntryScreen(
            budgetId = route.budgetId.takeIf { it != -1L },
            viewModel = entryViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<BudgetDetailRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<BudgetDetailRoute>()
        val budgetViewModel: BudgetViewModel = hiltViewModel()
        val baseCurrency by vms.settingsViewModel.currency.collectAsStateWithLifecycle()
        BudgetDetailScreen(
            budgetId = route.budgetId,
            viewModel = budgetViewModel,
            accountViewModel = vms.accountViewModel,
            transactionViewModel = vms.transactionViewModel,
            baseCurrency = baseCurrency,
            onEditClick = { id -> navController.navigate(BudgetEntryRoute(budgetId = id)) },
            onBackClick = { navController.popBackStack() },
            onTransactionClick = { id -> navController.navigate(TransactionEntryRoute(transactionId = id)) },
            onCloneTransactionClick = { id ->
                vms.transactionViewModel.setCloneTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            onSplitTransactionClick = { id ->
                vms.transactionViewModel.setSplitTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            onRecordsClick = { filter -> navController.navigateToRecords(filter, topLevel = false) }
        )
    }

    // ── Loans ─────────────────────────────────────────────────────────────────

    composable<LoanListRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        val loanViewModel: LoanViewModel = hiltViewModel()
        LoanListScreen(
            viewModel = loanViewModel,
            onAddClick = { navController.navigate(LoanEntryRoute()) },
            onLoanClick = { id -> navController.navigate(LoanDetailRoute(id)) },
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<LoanEntryRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<LoanEntryRoute>()
        val entryViewModel: LoanEntryViewModel = hiltViewModel()
        LoanEntryScreen(
            loanId = route.loanId.takeIf { it != -1L },
            viewModel = entryViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<LoanDetailRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<LoanDetailRoute>()
        val loanViewModel: LoanViewModel = hiltViewModel()
        val baseCurrency by vms.settingsViewModel.currency.collectAsStateWithLifecycle()
        LoanDetailScreen(
            loanId = route.loanId,
            viewModel = loanViewModel,
            accountViewModel = vms.accountViewModel,
            transactionViewModel = vms.transactionViewModel,
            baseCurrency = baseCurrency,
            onEditClick = { navController.navigate(LoanEntryRoute(loanId = route.loanId)) },
            onBackClick = { navController.popBackStack() },
            onTransactionClick = { id -> navController.navigate(TransactionEntryRoute(transactionId = id)) },
            onCloneTransactionClick = { id ->
                vms.transactionViewModel.setCloneTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            onSplitTransactionClick = { id ->
                vms.transactionViewModel.setSplitTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            onRecordsClick = { filter -> navController.navigateToRecords(filter, topLevel = false) }
        )
    }
}
