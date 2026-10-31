package com.osscabinet.ossWallet.ui.navigation.graphs

import androidx.compose.material3.DrawerState
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.osscabinet.ossWallet.ui.navigation.SmartSuggestionsRoute
import com.osscabinet.ossWallet.ui.navigation.AccountDetailRoute
import com.osscabinet.ossWallet.ui.navigation.AccountListRoute
import com.osscabinet.ossWallet.ui.navigation.AnalyticsRoute
import com.osscabinet.ossWallet.ui.navigation.BudgetDetailRoute
import com.osscabinet.ossWallet.ui.navigation.BudgetListRoute
import com.osscabinet.ossWallet.ui.navigation.DashboardRoute
import com.osscabinet.ossWallet.ui.navigation.EnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.ExchangeRatesRoute
import com.osscabinet.ossWallet.ui.navigation.ExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.FadeEnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.FadeExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.FilterEntryRoute
import com.osscabinet.ossWallet.ui.navigation.LoanDetailRoute
import com.osscabinet.ossWallet.ui.navigation.GoalListRoute
import com.osscabinet.ossWallet.ui.navigation.LoanListRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentDetailRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentListRoute
import com.osscabinet.ossWallet.ui.navigation.PopEnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.PopExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.navigation.TransactionEntryRoute
import com.osscabinet.ossWallet.ui.navigation.TransactionListRoute
import com.osscabinet.ossWallet.ui.navigation.navigateToRecords
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsActions
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsConfig
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsScreen
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardActions
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardConfig
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardScreen
import com.osscabinet.ossWallet.ui.screens.settings.ExchangeRatesActions
import com.osscabinet.ossWallet.ui.screens.settings.ExchangeRatesConfig
import com.osscabinet.ossWallet.ui.screens.settings.ExchangeRatesScreen
import com.osscabinet.ossWallet.ui.screens.transactions.list.TransactionListActions
import com.osscabinet.ossWallet.ui.screens.transactions.list.TransactionListConfig
import com.osscabinet.ossWallet.ui.screens.transactions.list.TransactionListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * True when this entry is the Records *tab* (reached from the drawer) rather than a Records
 * screen pushed from a card or filter — the two want different transitions. Defensive against
 * entries that aren't TransactionListRoute at all, since transition lambdas also see the other
 * side of the animation.
 */
private fun NavBackStackEntry.isTopLevelRecords(): Boolean =
    runCatching { toRoute<TransactionListRoute>().topLevel }.getOrDefault(false)

fun NavGraphBuilder.primaryTabsGraph(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    vms: SharedNavViewModels,
) {
    composable<DashboardRoute>(
        enterTransition = { FadeEnterTransitionSpec },
        exitTransition = { FadeExitTransitionSpec },
        popEnterTransition = { FadeEnterTransitionSpec },
        popExitTransition = { FadeExitTransitionSpec },
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<DashboardRoute>()
        DashboardScreen(
            config = DashboardConfig(
                isTopLevel = route.topLevel,
                viewContextJson = route.viewContextJson,
            ),
            actions = DashboardActions(
                onMenuClick = { scope.launch { drawerState.open() } },
                onNavigateToSmartSuggestions = { navController.navigate(SmartSuggestionsRoute) },
                onBackClick = { navController.popBackStack() },
                onRecordsClick = { filter ->
                    navController.navigateToRecords(filter, topLevel = false)
                },
                onAddClick = { type, templateId ->
                    navController.navigate(
                        if (templateId != null) TransactionEntryRoute(templateId = templateId)
                        else TransactionEntryRoute(transactionType = type?.ordinal ?: -1)
                    )
                },
                onManageAccountsClick = { navController.navigate(AccountListRoute) },
                onAccountClick = { id -> navController.navigate(AccountDetailRoute(accountId = id)) },
                onNavigateToPlannedPaymentList = { navController.navigate(PlannedPaymentListRoute) },
                onNavigateToBudgetList = { navController.navigate(BudgetListRoute) },
                onNavigateToLoanList = { navController.navigate(LoanListRoute) },
                onNavigateToGoalList = { navController.navigate(GoalListRoute) },
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
                onSaveFilterClick = { filter ->
                    val encoded = URLEncoder.encode(
                        Json.encodeToString(filter),
                        StandardCharsets.UTF_8.toString()
                    )
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
            ),
            vms = vms
        )
    }

    // TransactionListRoute is dual-purpose: a drawer sibling tab when topLevel, and a pushed
    // detail screen when opened from a card/filter. As a sibling tab it gets the same short fade
    // as Dashboard/Analytics/ExchangeRates — a 350ms slide there was both the wrong motion
    // (lateral switch, not a drill-down) and the longest window during which Navigation Compose's
    // RESUMED-gate makes a rapid switch-back wait. Pushed, it keeps the expressive slide.
    composable<TransactionListRoute>(
        enterTransition = {
            if (targetState.isTopLevelRecords()) FadeEnterTransitionSpec else EnterTransitionSpec
        },
        exitTransition = {
            if (initialState.isTopLevelRecords()) FadeExitTransitionSpec else ExitTransitionSpec
        },
        popEnterTransition = {
            if (targetState.isTopLevelRecords()) FadeEnterTransitionSpec else PopEnterTransitionSpec
        },
        popExitTransition = {
            if (initialState.isTopLevelRecords()) FadeExitTransitionSpec else PopExitTransitionSpec
        }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<TransactionListRoute>()
        TransactionListScreen(
            config = TransactionListConfig(
                isTopLevel = route.topLevel,
                filterJson = route.filterJson.ifEmpty { null }
            ),
            actions = TransactionListActions(
                onMenuClick = { scope.launch { drawerState.open() } },
                onBackClick = { navController.popBackStack() },
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
                onSaveFilterClick = { filter ->
                    val encoded = URLEncoder.encode(
                        Json.encodeToString(filter),
                        StandardCharsets.UTF_8.toString()
                    )
                    navController.navigate(FilterEntryRoute(initialFilterJson = encoded))
                }
            ),
            vms = vms
        )
    }

    composable<AnalyticsRoute>(
        enterTransition = { FadeEnterTransitionSpec },
        exitTransition = { FadeExitTransitionSpec },
        popEnterTransition = { FadeEnterTransitionSpec },
        popExitTransition = { FadeExitTransitionSpec }
    ) {
        AnalyticsScreen(
            config = AnalyticsConfig(
                isTopLevel = true
            ),
            actions = AnalyticsActions(
                onMenuClick = { scope.launch { drawerState.open() } },
                onBackClick = { navController.popBackStack() },
                onRecordsClick = { filter ->
                    navController.navigateToRecords(
                        filter,
                        topLevel = false
                    )
                },
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
            ),
            vms = vms
        )
    }

    composable<ExchangeRatesRoute>(
        enterTransition = { FadeEnterTransitionSpec },
        exitTransition = { FadeExitTransitionSpec },
        popEnterTransition = { FadeEnterTransitionSpec },
        popExitTransition = { FadeExitTransitionSpec }
    ) {
        ExchangeRatesScreen(
            config = ExchangeRatesConfig(
                isTopLevel = true
            ),
            actions = ExchangeRatesActions(
                onMenuClick = { scope.launch { drawerState.open() } },
                onBackClick = { navController.popBackStack() }
            ),
            vms = vms
        )
    }
}
