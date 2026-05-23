package com.osscabinet.ossWallet.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AppDrawerContent
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.navigation.Screen
import com.osscabinet.ossWallet.ui.navigation.navigateToDashboard
import com.osscabinet.ossWallet.ui.navigation.navigateToRecords
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsScreen
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardScreen
import com.osscabinet.ossWallet.ui.screens.settings.ExchangeRatesScreen
import com.osscabinet.ossWallet.ui.screens.transactions.list.TransactionListScreen
import kotlinx.coroutines.launch

@Composable
fun MainDrawerShell(
    rootNavController: NavHostController,
    mainViewModel: MainViewModel,
    initialTab: String? = null,
    // Dependency injection for viewmodels needed by nested screens
    dashboardViewModel: com.osscabinet.ossWallet.ui.screens.dashboard.DashboardViewModel,
    transactionViewModel: com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel,
    accountViewModel: com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel,
    analyticsViewModel: com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsViewModel,
    currencyViewModel: com.osscabinet.ossWallet.ui.screens.settings.CurrencyViewModel,
    templateViewModel: com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel,
    settingsViewModel: com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
) {
    val nestedNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isDrawerNavigating by remember { mutableStateOf(false) }
    var edgeDragTotal by remember { mutableFloatStateOf(0f) }
    val openThresholdPx = with(LocalDensity.current) { 60.dp.toPx() }
    val currentIsDrawerNavigating by rememberUpdatedState(isDrawerNavigating)
    val currentOpenThresholdPx by rememberUpdatedState(openThresholdPx)

    val currentProfile by mainViewModel.currentProfile.collectAsStateWithLifecycle()
    val nonArchivedAccounts by mainViewModel.nonArchivedAccounts.collectAsStateWithLifecycle()
    val starredTemplates by mainViewModel.starredTemplates.collectAsStateWithLifecycle()

    val currentBackStackEntry by nestedNavController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route?.substringBefore("?")
        ?: Screen.Dashboard.route.substringBefore("?")

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    LaunchedEffect(initialTab) {
        initialTab?.let { tab ->
            val route = when (tab) {
                "dashboard" -> Screen.Dashboard.route
                "records" -> Screen.TransactionList.route
                "analytics" -> Screen.Analytics.route
                "exchange_rates" -> Screen.ExchangeRates.route
                else -> null
            }
            route?.let {
                nestedNavController.navigate(it) {
                    popUpTo(nestedNavController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false, // edge-only swipe handled by pointerInput below
        drawerContent = {
            AppDrawerContent(
                currentProfile = currentProfile,
                nonArchivedAccounts = nonArchivedAccounts,
                starredTemplates = starredTemplates,
                selectedDestination = currentDestination,
                onDashboardClick = {
                    nestedNavController.navigateToDashboard(
                        context = DashboardViewContext.None,
                        topLevel = true
                    ) {
                        popUpTo(nestedNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onRecordsClick = {
                    nestedNavController.navigateToRecords(
                        filter = TransactionFilter(),
                        topLevel = true
                    ) {
                        popUpTo(nestedNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAnalyticsClick = {
                    nestedNavController.navigate(Screen.Analytics.createRoute(topLevel = true)) {
                        popUpTo(nestedNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onExchangeRatesClick = {
                    nestedNavController.navigate(Screen.ExchangeRates.createRoute(topLevel = true)) {
                        popUpTo(nestedNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSettingsClick = {
                    rootNavController.navigate(Screen.Settings.route)
                },
                onAccountClick = { id ->
                    rootNavController.navigate(Screen.AccountDetail.createRoute(id))
                },
                onTemplateClick = { template ->
                    rootNavController.navigate(
                        Screen.TransactionEntry.createRoute(
                            type = template.transactionType,
                            templateId = template.id
                        )
                    )
                },
                onPlannedPaymentsClick = {
                    rootNavController.navigate(Screen.PlannedPaymentList.route)
                },
                onBudgetsClick = {
                    rootNavController.navigate(Screen.BudgetList.route)
                },
                onLoansClick = {
                    rootNavController.navigate(Screen.LoanList.route)
                },
                onNavigate = { action ->
                    if (!isDrawerNavigating) {
                        isDrawerNavigating = true
                        scope.launch {
                            drawerState.close()
                            action()
                            isDrawerNavigating = false
                        }
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = nestedNavController,
                startDestination = Screen.Dashboard.route
            ) {
                composable(
                    route = Screen.Dashboard.route,
                    arguments = listOf(
                        navArgument("viewContext") {
                            type = NavType.StringType; nullable = true; defaultValue = null
                        },
                        navArgument("topLevel") { type = NavType.BoolType; defaultValue = true }
                    )
                ) { backStackEntry ->
                    val viewContext = backStackEntry.arguments?.getString("viewContext")
                    DashboardScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onBackClick = { rootNavController.popBackStack() },
                        isTopLevelParam = true,
                        onRecordsClick = { filter ->
                            nestedNavController.navigateToRecords(
                                filter,
                                topLevel = true
                            )
                        },
                        onAddClick = { type, templateId ->
                            if (templateId != null) {
                                transactionViewModel.setTemplatePrefill(templateId)
                                rootNavController.navigate(Screen.TransactionEntry.createRoute())
                            } else {
                                rootNavController.navigate(Screen.TransactionEntry.createRoute(type = type))
                            }
                        },
                        onManageAccountsClick = { rootNavController.navigate(Screen.AccountList.route) },
                        onAccountClick = { id ->
                            rootNavController.navigate(
                                Screen.AccountDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onNavigateToPlannedPaymentList = { rootNavController.navigate(Screen.PlannedPaymentList.route) },
                        onNavigateToBudgetList = { rootNavController.navigate(Screen.BudgetList.route) },
                        onNavigateToLoanList = { rootNavController.navigate(Screen.LoanList.route) },
                        onNavigateToPlannedPaymentDetail = { id ->
                            rootNavController.navigate(
                                Screen.PlannedPaymentDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onNavigateToBudgetDetail = { id ->
                            rootNavController.navigate(
                                Screen.BudgetDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onNavigateToLoanDetail = { id ->
                            rootNavController.navigate(
                                Screen.LoanDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onSettingsClick = { rootNavController.navigate(Screen.Settings.route) },
                        onEditTransactionClick = { id ->
                            rootNavController.navigate(
                                Screen.TransactionEntry.createRoute(
                                    transactionId = id
                                )
                            )
                        },
                        onSaveFilterClick = { filter ->
                            // Using root because FilterEntry is in root
                            val filterJson = java.net.URLEncoder.encode(
                                kotlinx.serialization.json.Json.encodeToString(filter), "UTF-8"
                            )
                            rootNavController.navigate(Screen.FilterEntry.createRoute(initialFilter = filterJson))
                        },
                        onCloneTransactionClick = { id ->
                            transactionViewModel.setCloneTransactionId(id)
                            rootNavController.navigate(Screen.TransactionEntry.createRoute())
                        },
                        onSplitTransactionClick = { id ->
                            transactionViewModel.setSplitTransactionId(id)
                            rootNavController.navigate(Screen.TransactionEntry.createRoute())
                        },
                        accountViewModel = accountViewModel,
                        transactionViewModel = transactionViewModel,
                        settingsViewModel = settingsViewModel,
                        analyticsViewModel = analyticsViewModel,
                        templateViewModel = templateViewModel,
                        dashboardViewModel = dashboardViewModel,
                        cardComputerRegistry = dashboardViewModel.cardComputerRegistry,
                        viewContext = viewContext
                    )
                }

                composable(
                    route = Screen.TransactionList.route,
                    arguments = listOf(
                        navArgument("filter") {
                            type = NavType.StringType; nullable = true; defaultValue = null
                        },
                        navArgument("topLevel") { type = NavType.BoolType; defaultValue = true }
                    )
                ) { backStackEntry ->
                    val filterJson = backStackEntry.arguments?.getString("filter")
                    TransactionListScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        isTopLevelParam = true,
                        viewModel = transactionViewModel,
                        accountViewModel = accountViewModel,
                        analyticsViewModel = analyticsViewModel,
                        settingsViewModel = settingsViewModel,
                        onEditTransactionClick = { id ->
                            rootNavController.navigate(
                                Screen.TransactionEntry.createRoute(
                                    transactionId = id
                                )
                            )
                        },
                        onCloneTransactionClick = { id ->
                            transactionViewModel.setCloneTransactionId(id)
                            rootNavController.navigate(Screen.TransactionEntry.createRoute())
                        },
                        onSplitTransactionClick = { id ->
                            transactionViewModel.setSplitTransactionId(id)
                            rootNavController.navigate(Screen.TransactionEntry.createRoute())
                        },
                        onBackClick = { nestedNavController.popBackStack() },
                        onSaveFilterClick = { filter ->
                            val filterJson = java.net.URLEncoder.encode(
                                kotlinx.serialization.json.Json.encodeToString(filter), "UTF-8"
                            )
                            rootNavController.navigate(Screen.FilterEntry.createRoute(initialFilter = filterJson))
                        },
                        templateViewModel = templateViewModel,
                        filterJson = filterJson
                    )
                }

                composable(
                    route = Screen.Analytics.route,
                    arguments = listOf(
                        navArgument("topLevel") { type = NavType.BoolType; defaultValue = true }
                    )
                ) {
                    AnalyticsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onBackClick = { nestedNavController.popBackStack() },
                        isTopLevel = true,
                        viewModel = analyticsViewModel,
                        accountViewModel = accountViewModel,
                        transactionViewModel = transactionViewModel,
                        templateViewModel = templateViewModel,
                        settingsViewModel = settingsViewModel,
                        onRecordsClick = { filter ->
                            nestedNavController.navigateToRecords(
                                filter,
                                topLevel = true
                            )
                        },
                        onAccountClick = { id ->
                            rootNavController.navigate(
                                Screen.AccountDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onNavigateToPlannedPaymentList = { rootNavController.navigate(Screen.PlannedPaymentList.route) },
                        onNavigateToBudgetList = { rootNavController.navigate(Screen.BudgetList.route) },
                        onNavigateToLoanList = { rootNavController.navigate(Screen.LoanList.route) },
                        onNavigateToPlannedPaymentDetail = { id ->
                            rootNavController.navigate(
                                Screen.PlannedPaymentDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onNavigateToBudgetDetail = { id ->
                            rootNavController.navigate(
                                Screen.BudgetDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onNavigateToLoanDetail = { id ->
                            rootNavController.navigate(
                                Screen.LoanDetail.createRoute(
                                    id
                                )
                            )
                        },
                        onEditTransactionClick = { id ->
                            rootNavController.navigate(
                                Screen.TransactionEntry.createRoute(
                                    transactionId = id
                                )
                            )
                        },
                        onCloneTransactionClick = { id ->
                            transactionViewModel.setCloneTransactionId(id)
                            rootNavController.navigate(Screen.TransactionEntry.createRoute())
                        },
                        onSplitTransactionClick = { id ->
                            transactionViewModel.setSplitTransactionId(id)
                            rootNavController.navigate(Screen.TransactionEntry.createRoute())
                        },
                        cardComputerRegistry = dashboardViewModel.cardComputerRegistry
                    )
                }

                composable(
                    route = Screen.ExchangeRates.route,
                    arguments = listOf(
                        navArgument("topLevel") { type = NavType.BoolType; defaultValue = true }
                    )
                ) {
                    ExchangeRatesScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onBackClick = { nestedNavController.popBackStack() },
                        isTopLevel = true,
                        viewModel = currencyViewModel
                    )
                }
            }

            // Transparent 20dp edge strip — any drag here is always an edge drag,
            // no coordinate math needed. Sits above NavHost so it wins gesture priority.
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .pointerInput(drawerState) {
                        detectHorizontalDragGestures(
                            onDragStart = { edgeDragTotal = 0f },
                            onDragEnd = {
                                if (!currentIsDrawerNavigating) {
                                    scope.launch {
                                        if (edgeDragTotal > currentOpenThresholdPx) drawerState.open()
                                        else drawerState.close()
                                    }
                                }
                                edgeDragTotal = 0f
                            },
                            onDragCancel = { edgeDragTotal = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                edgeDragTotal += dragAmount
                            }
                        )
                    }
            )
        }
    }
}
