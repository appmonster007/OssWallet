package com.osscabinet.ossWallet.ui.navigation.graphs

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.osscabinet.ossWallet.ui.navigation.AccountDetailRoute
import com.osscabinet.ossWallet.ui.navigation.AccountEntryRoute
import com.osscabinet.ossWallet.ui.navigation.AccountListRoute
import com.osscabinet.ossWallet.ui.navigation.BudgetDetailRoute
import com.osscabinet.ossWallet.ui.navigation.BudgetListRoute
import com.osscabinet.ossWallet.ui.navigation.EnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.ExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.FilterEntryRoute
import com.osscabinet.ossWallet.ui.navigation.LoanDetailRoute
import com.osscabinet.ossWallet.ui.navigation.LoanListRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentDetailRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentListRoute
import com.osscabinet.ossWallet.ui.navigation.PopEnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.PopExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.navigation.TransactionEntryRoute
import com.osscabinet.ossWallet.ui.navigation.navigateToRecords
import com.osscabinet.ossWallet.ui.screens.accounts.detail.AccountDetailActions
import com.osscabinet.ossWallet.ui.screens.accounts.detail.AccountDetailConfig
import com.osscabinet.ossWallet.ui.screens.accounts.detail.AccountDetailScreen
import com.osscabinet.ossWallet.ui.screens.accounts.detail.AccountDetailViewModel
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryActions
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryConfig
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryScreen
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryViewModel
import com.osscabinet.ossWallet.ui.screens.accounts.list.AccountListActions
import com.osscabinet.ossWallet.ui.screens.accounts.list.AccountListScreen
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun NavGraphBuilder.accountsGraph(
    navController: NavHostController,
    vms: SharedNavViewModels,
) {
    composable<AccountListRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        AccountListScreen(
            actions = AccountListActions(
                onBackClick = { navController.popBackStack() },
                onAddAccountClick = { navController.navigate(AccountEntryRoute()) },
                onAccountClick = { id -> navController.navigate(AccountDetailRoute(accountId = id)) }
            ),
            viewModel = vms.accountViewModel
        )
    }

    composable<AccountDetailRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<AccountDetailRoute>()
        val accountDetailViewModel: AccountDetailViewModel = hiltViewModel()
        AccountDetailScreen(
            config = AccountDetailConfig(
                accountId = route.accountId,
                filterJson = route.filterJson
            ),
            actions = AccountDetailActions(
                onBackClick = { navController.popBackStack() },
                onEditAccountClick = { id -> navController.navigate(AccountEntryRoute(accountId = id)) },
                onAccountClick = { id -> navController.navigate(AccountDetailRoute(accountId = id)) },
                onNavigateToPlannedPaymentList = { navController.navigate(PlannedPaymentListRoute) },
                onNavigateToBudgetList = { navController.navigate(BudgetListRoute) },
                onNavigateToLoanList = { navController.navigate(LoanListRoute) },
                onNavigateToPlannedPaymentDetail = { id ->
                    navController.navigate(
                        PlannedPaymentDetailRoute(id)
                    )
                },
                onNavigateToBudgetDetail = { id -> navController.navigate(BudgetDetailRoute(id)) },
                onNavigateToLoanDetail = { id -> navController.navigate(LoanDetailRoute(id)) },
                onEditTransactionClick = { id ->
                    navController.navigate(
                        TransactionEntryRoute(
                            transactionId = id
                        )
                    )
                },
                onCloneTransactionClick = { id ->
                    vms.transactionViewModel.setCloneTransactionId(id)
                    navController.navigate(TransactionEntryRoute())
                },
                onSplitTransactionClick = { id ->
                    vms.transactionViewModel.setSplitTransactionId(id)
                    navController.navigate(TransactionEntryRoute())
                },
                onAddTransactionClick = { accId, type, templateId ->
                    navController.navigate(
                        TransactionEntryRoute(
                            accountId = accId,
                            transactionType = type?.ordinal ?: -1,
                            templateId = templateId ?: -1L
                        )
                    )
                },
                onRecordsClick = { filter ->
                    navController.navigateToRecords(
                        filter,
                        topLevel = false
                    )
                },
                onSaveFilterClick = { filter ->
                    val encoded = URLEncoder.encode(
                        Json.encodeToString(filter),
                        StandardCharsets.UTF_8.toString()
                    )
                    navController.navigate(FilterEntryRoute(initialFilterJson = encoded))
                }
            ),
            vms = vms,
            accountDetailViewModel = accountDetailViewModel
        )
    }

    composable<AccountEntryRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<AccountEntryRoute>()
        val entryViewModel: AccountEntryViewModel = hiltViewModel()
        AccountEntryScreen(
            config = AccountEntryConfig(accountId = route.accountId),
            actions = AccountEntryActions(
                onBackClick = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            ),
            viewModel = entryViewModel
        )
    }
}
