package com.osscabinet.ossWallet.ui.navigation

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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.osscabinet.ossWallet.ui.components.AppDrawerContent
import com.osscabinet.ossWallet.ui.navigation.graphs.accountsGraph
import com.osscabinet.ossWallet.ui.navigation.graphs.planningGraph
import com.osscabinet.ossWallet.ui.navigation.graphs.primaryTabsGraph
import com.osscabinet.ossWallet.ui.navigation.graphs.settingsGraph
import com.osscabinet.ossWallet.ui.navigation.graphs.transactionsGraph
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.screens.categories.CategoryViewModel
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardUiState
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardViewModel
import com.osscabinet.ossWallet.ui.screens.main.MainViewModel
import com.osscabinet.ossWallet.ui.screens.onboarding.OnboardingScreen
import com.osscabinet.ossWallet.ui.screens.onboarding.OnboardingViewModel
import com.osscabinet.ossWallet.ui.screens.settings.CurrencyViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.sharedwallet.SharedWalletViewModel
import com.osscabinet.ossWallet.ui.screens.tags.TagViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import kotlinx.coroutines.launch

@Composable
fun OssWalletNavGraph(
    navController: NavHostController,
    onSplashConditionChanged: (Boolean) -> Unit = {}
) {
    val vms = SharedNavViewModels(
        settingsViewModel = hiltViewModel(),
        analyticsViewModel = hiltViewModel(),
        categoryViewModel = hiltViewModel(),
        transactionViewModel = hiltViewModel(),
        accountViewModel = hiltViewModel(),
        sharedWalletViewModel = hiltViewModel(),
        templateViewModel = hiltViewModel(),
        currencyViewModel = hiltViewModel(),
        dashboardViewModel = hiltViewModel(),
        tagViewModel = hiltViewModel(),
        mainViewModel = hiltViewModel()
    )

    val dashboardUiState by vms.dashboardViewModel.uiState.collectAsStateWithLifecycle()

    // Lock startDestination once — suspend until DataStore emits the first real value.
    // Using LaunchedEffect(Unit) ensures this runs exactly once per composition lifecycle,
    // and filterNotNull().first() suspends on the main thread without blocking it.
    var startDestination by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(Unit) {
        val completed = vms.settingsViewModel.isOnboardingCompleted
            .filterNotNull()
            .first()
        startDestination = if (completed) DashboardRoute() else OnboardingRoute
    }

    // Keep the splash visible until startDestination is resolved AND (if going to Dashboard)
    // the initial data load completes.
    SideEffect {
        onSplashConditionChanged(
            startDestination == null ||
                (startDestination is DashboardRoute && dashboardUiState is DashboardUiState.Loading)
        )
    }

    // Splash covers the blank frame while DataStore resolves (<50ms on any device).
    // Captured as a local val so Kotlin can smart-cast it (delegated props can't be smart-cast).
    val resolvedDestination = startDestination ?: return

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isDrawerNavigating by remember { mutableStateOf(false) }
    var edgeDragTotal by remember { mutableFloatStateOf(0f) }
    val openThresholdPx = with(LocalDensity.current) { 60.dp.toPx() }
    val currentIsDrawerNavigating by rememberUpdatedState(isDrawerNavigating)
    val currentOpenThresholdPx by rememberUpdatedState(openThresholdPx)

    val currentProfile by vms.mainViewModel.currentProfile.collectAsStateWithLifecycle()
    val nonArchivedAccounts by vms.mainViewModel.nonArchivedAccounts.collectAsStateWithLifecycle()
    val starredTemplates by vms.mainViewModel.starredTemplates.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val selectedRoute = when {
        currentDest?.hasRoute(DashboardRoute::class) == true          -> "dashboard"
        currentDest?.hasRoute(TransactionListRoute::class) == true    -> "transaction_list"
        currentDest?.hasRoute(AnalyticsRoute::class) == true          -> "analytics"
        currentDest?.hasRoute(ExchangeRatesRoute::class) == true      -> "exchange_rates"
        currentDest?.hasRoute(SettingsRoute::class) == true           -> "settings"
        currentDest?.hasRoute(PlannedPaymentListRoute::class) == true -> "planned_payment_list"
        currentDest?.hasRoute(BudgetListRoute::class) == true         -> "budget_list"
        currentDest?.hasRoute(LoanListRoute::class) == true           -> "loan_list"
        else                                                           -> ""
    }

    // Back on any non-Dashboard top-level screen → navigate to Dashboard (never exit from those tabs)
    val isOnNonDashboardTopLevel = when {
        currentDest?.hasRoute(AnalyticsRoute::class) == true     -> true
        currentDest?.hasRoute(ExchangeRatesRoute::class) == true -> true
        currentDest?.hasRoute(TransactionListRoute::class) == true -> {
            navBackStackEntry?.toRoute<TransactionListRoute>()?.topLevel == true
        }
        else -> false
    }
    BackHandler(enabled = isOnNonDashboardTopLevel && !drawerState.isOpen) {
        navController.navigate(DashboardRoute()) {
            popUpTo<DashboardRoute> { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Drawer close takes priority — registered second so it fires first when open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            AppDrawerContent(
                currentProfile = currentProfile,
                nonArchivedAccounts = nonArchivedAccounts,
                starredTemplates = starredTemplates,
                selectedDestination = selectedRoute,
                onDashboardClick = {
                    navController.navigateToDashboard(topLevel = true) {
                        popUpTo<DashboardRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onRecordsClick = {
                    navController.navigateToRecords(filter = TransactionFilter(), topLevel = true) {
                        popUpTo<DashboardRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAnalyticsClick = {
                    navController.navigate(AnalyticsRoute) {
                        popUpTo<DashboardRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onExchangeRatesClick = {
                    navController.navigate(ExchangeRatesRoute) {
                        popUpTo<DashboardRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSettingsClick = { navController.navigate(SettingsRoute) },
                onAccountClick = { id -> navController.navigate(AccountDetailRoute(accountId = id)) },
                onTemplateClick = { template ->
                    vms.transactionViewModel.setTemplatePrefill(template.id)
                    navController.navigate(TransactionEntryRoute())
                },
                onPlannedPaymentsClick = { navController.navigate(PlannedPaymentListRoute) },
                onBudgetsClick = { navController.navigate(BudgetListRoute) },
                onLoansClick = { navController.navigate(LoanListRoute) },
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
                navController = navController,
                startDestination = resolvedDestination
            ) {
                composable<OnboardingRoute>(
                    enterTransition = { FadeEnterTransitionSpec },
                    exitTransition = { FadeExitTransitionSpec },
                    popEnterTransition = { FadeEnterTransitionSpec },
                    popExitTransition = { FadeExitTransitionSpec }
                ) {
                    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        onComplete = {
                            navController.navigate(DashboardRoute()) {
                                popUpTo<OnboardingRoute> { inclusive = true }
                            }
                        }
                    )
                }

                primaryTabsGraph(navController, drawerState, scope, vms)
                settingsGraph(navController, vms)
                accountsGraph(navController, vms)
                transactionsGraph(navController, vms)
                planningGraph(navController, vms)
            }

            // Transparent 20dp edge strip — any drag here is always an edge drag.
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
