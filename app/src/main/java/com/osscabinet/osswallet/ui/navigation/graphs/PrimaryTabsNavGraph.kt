package com.osscabinet.ossWallet.ui.navigation.graphs

import androidx.compose.material3.DrawerState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.osscabinet.ossWallet.ui.navigation.AccountDetailRoute
import com.osscabinet.ossWallet.ui.navigation.AccountListRoute
import com.osscabinet.ossWallet.ui.navigation.AnalyticsRoute
import com.osscabinet.ossWallet.ui.navigation.BudgetDetailRoute
import com.osscabinet.ossWallet.ui.navigation.BudgetListRoute
import com.osscabinet.ossWallet.ui.navigation.DashboardRoute
import com.osscabinet.ossWallet.ui.navigation.EnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.ExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.ExchangeRatesRoute
import com.osscabinet.ossWallet.ui.navigation.FadeEnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.FadeExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.FilterEntryRoute
import com.osscabinet.ossWallet.ui.navigation.LoanDetailRoute
import com.osscabinet.ossWallet.ui.navigation.LoanListRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentDetailRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentListRoute
import com.osscabinet.ossWallet.ui.navigation.PopEnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.PopExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.navigation.TransactionEntryRoute
import com.osscabinet.ossWallet.ui.navigation.TransactionListRoute
import com.osscabinet.ossWallet.ui.navigation.navigateToRecords
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsScreen
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardScreen
import com.osscabinet.ossWallet.ui.screens.settings.ExchangeRatesScreen
import com.osscabinet.ossWallet.ui.screens.transactions.list.TransactionListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun NavGraphBuilder.primaryTabsGraph(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    vms: SharedNavViewModels
) {
    composable<DashboardRoute>(
        enterTransition = { FadeEnterTransitionSpec },
        exitTransition = { FadeExitTransitionSpec },
        popEnterTransition = { FadeEnterTransitionSpec },
        popExitTransition = { FadeExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<DashboardRoute>()
        DashboardScreen(
            onMenuClick = { scope.launch { drawerState.open() } },
            onBackClick = { navController.popBackStack() },
            isTopLevelParam = route.topLevel,
            viewContext = route.viewContextJson,
            onRecordsClick = { filter ->
                navController.navigateToRecords(filter, topLevel = false)
            },
            onAddClick = { type, templateId ->
                if (templateId != null) {
                    vms.transactionViewModel.setTemplatePrefill(templateId)
                    navController.navigate(TransactionEntryRoute())
                } else {
                    navController.navigate(TransactionEntryRoute(transactionType = type?.toInt() ?: -1))
                }
            },
            onManageAccountsClick = { navController.navigate(AccountListRoute) },
            onAccountClick = { id -> navController.navigate(AccountDetailRoute(accountId = id)) },
            onNavigateToPlannedPaymentList = { navController.navigate(PlannedPaymentListRoute) },
            onNavigateToBudgetList = { navController.navigate(BudgetListRoute) },
            onNavigateToLoanList = { navController.navigate(LoanListRoute) },
            onNavigateToPlannedPaymentDetail = { id -> navController.navigate(PlannedPaymentDetailRoute(id)) },
            onNavigateToBudgetDetail = { id -> navController.navigate(BudgetDetailRoute(id)) },
            onNavigateToLoanDetail = { id -> navController.navigate(LoanDetailRoute(id)) },
            onEditTransactionClick = { id -> navController.navigate(TransactionEntryRoute(transactionId = id)) },
            onSaveFilterClick = { filter ->
                val encoded = URLEncoder.encode(Json.encodeToString(filter), StandardCharsets.UTF_8.toString())
                navController.navigate(FilterEntryRoute(initialFilterJson = encoded))
            },
            onCloneTransactionClick = { id ->
                vms.transactionViewModel.setCloneTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            onSplitTransactionClick = { id ->
                vms.transactionViewModel.setSplitTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            accountViewModel = vms.accountViewModel,
            transactionViewModel = vms.transactionViewModel,
            settingsViewModel = vms.settingsViewModel,
            analyticsViewModel = vms.analyticsViewModel,
            templateViewModel = vms.templateViewModel,
            dashboardViewModel = vms.dashboardViewModel,
            cardComputerRegistry = vms.dashboardViewModel.cardComputerRegistry,
        )
    }

    composable<TransactionListRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<TransactionListRoute>()
        TransactionListScreen(
            onMenuClick = { scope.launch { drawerState.open() } },
            isTopLevelParam = route.topLevel,
            viewModel = vms.transactionViewModel,
            accountViewModel = vms.accountViewModel,
            analyticsViewModel = vms.analyticsViewModel,
            settingsViewModel = vms.settingsViewModel,
            onEditTransactionClick = { id -> navController.navigate(TransactionEntryRoute(transactionId = id)) },
            onCloneTransactionClick = { id ->
                vms.transactionViewModel.setCloneTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            onSplitTransactionClick = { id ->
                vms.transactionViewModel.setSplitTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            onBackClick = { navController.popBackStack() },
            onSaveFilterClick = { filter ->
                val encoded = URLEncoder.encode(Json.encodeToString(filter), StandardCharsets.UTF_8.toString())
                navController.navigate(FilterEntryRoute(initialFilterJson = encoded))
            },
            templateViewModel = vms.templateViewModel,
            filterJson = route.filterJson.ifEmpty { null }
        )
    }

    composable<AnalyticsRoute>(
        enterTransition = { FadeEnterTransitionSpec },
        exitTransition = { FadeExitTransitionSpec },
        popEnterTransition = { FadeEnterTransitionSpec },
        popExitTransition = { FadeExitTransitionSpec }
    ) {
        AnalyticsScreen(
            onMenuClick = { scope.launch { drawerState.open() } },
            onBackClick = { navController.popBackStack() },
            isTopLevel = true,
            viewModel = vms.analyticsViewModel,
            accountViewModel = vms.accountViewModel,
            transactionViewModel = vms.transactionViewModel,
            templateViewModel = vms.templateViewModel,
            settingsViewModel = vms.settingsViewModel,
            onRecordsClick = { filter -> navController.navigateToRecords(filter, topLevel = false) },
            onAccountClick = { id -> navController.navigate(AccountDetailRoute(accountId = id)) },
            onNavigateToPlannedPaymentList = { navController.navigate(PlannedPaymentListRoute) },
            onNavigateToBudgetList = { navController.navigate(BudgetListRoute) },
            onNavigateToLoanList = { navController.navigate(LoanListRoute) },
            onNavigateToPlannedPaymentDetail = { id -> navController.navigate(PlannedPaymentDetailRoute(id)) },
            onNavigateToBudgetDetail = { id -> navController.navigate(BudgetDetailRoute(id)) },
            onNavigateToLoanDetail = { id -> navController.navigate(LoanDetailRoute(id)) },
            onEditTransactionClick = { id -> navController.navigate(TransactionEntryRoute(transactionId = id)) },
            onCloneTransactionClick = { id ->
                vms.transactionViewModel.setCloneTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            onSplitTransactionClick = { id ->
                vms.transactionViewModel.setSplitTransactionId(id)
                navController.navigate(TransactionEntryRoute())
            },
            cardComputerRegistry = vms.dashboardViewModel.cardComputerRegistry
        )
    }

    composable<ExchangeRatesRoute>(
        enterTransition = { FadeEnterTransitionSpec },
        exitTransition = { FadeExitTransitionSpec },
        popEnterTransition = { FadeEnterTransitionSpec },
        popExitTransition = { FadeExitTransitionSpec }
    ) {
        ExchangeRatesScreen(
            onMenuClick = { scope.launch { drawerState.open() } },
            onBackClick = { navController.popBackStack() },
            isTopLevel = true,
            viewModel = vms.currencyViewModel
        )
    }
}
