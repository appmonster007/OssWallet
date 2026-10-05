package com.osscabinet.ossWallet.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
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
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardViewModel
import com.osscabinet.ossWallet.ui.screens.main.MainViewModel
import com.osscabinet.ossWallet.ui.screens.onboarding.OnboardingActions
import com.osscabinet.ossWallet.ui.screens.onboarding.OnboardingScreen
import com.osscabinet.ossWallet.ui.screens.onboarding.OnboardingViewModel
import com.osscabinet.ossWallet.ui.screens.settings.CurrencyViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.sharedwallet.SharedWalletViewModel
import com.osscabinet.ossWallet.ui.screens.tags.TagViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import kotlinx.coroutines.launch

@Composable
fun OssWalletNavGraph(
    navController: NavHostController,
    onSplashConditionChanged: (Boolean) -> Unit = {},
    pendingWidgetRoute: String? = null,
    onWidgetRouteHandled: () -> Unit = {},
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

    // Only gate the splash on the DataStore read (~50ms). Dashboard data loads after the splash
    // dismisses and shows AeonShimmer while it does — gating on dashboardUiState caused the splash
    // to hang indefinitely when the currency network call timed out on a stale cache.
    SideEffect {
        onSplashConditionChanged(startDestination == null)
    }

    // Splash covers the blank frame while DataStore resolves (<50ms on any device).
    // Captured as a local val so Kotlin can smart-cast it (delegated props can't be smart-cast).
    val resolvedDestination = startDestination ?: return

    val drawerHazeState = remember { HazeState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Funnels navigation through one place so NavOptions follow from the route's category —
    // see AppNavigator.
    val appNavigator = remember(navController) { AppNavigator(navController) }

    // Drawer-close runs independently of the navigation itself (not awaited/sequenced with it)
    // so the two animations overlap instead of stacking.
    fun navigateFromDrawer(route: Any, extraOptions: (androidx.navigation.NavOptionsBuilder.() -> Unit)? = null) {
        appNavigator.navigateTo(route, extraOptions)
        scope.launch { drawerState.close() }
    }

    // Widget taps land here too, now via AppNavigator — same RESUMED-wait/coalescing safety as
    // the drawer. NavOptions are fully explicit (not AppNavigator's TopLevelRoute default) to
    // preserve this path's existing saveState=false/no-restoreState behavior exactly; that
    // divergence from the drawer's save/restore semantics may be intentional (a widget tap
    // landing on a fresh, un-restored screen rather than resuming prior scroll/filter state)
    // and isn't this change's call to silently alter.
    LaunchedEffect(pendingWidgetRoute) {
        pendingWidgetRoute?.let { route ->
            val widgetNavOptions: androidx.navigation.NavOptionsBuilder.() -> Unit = {
                popUpTo<DashboardRoute> { inclusive = false; saveState = false }
                launchSingleTop = true
                restoreState = false
            }
            when (route) {
                "dashboard"      -> appNavigator.navigateTo(DashboardRoute(), widgetNavOptions)
                "records"        -> appNavigator.navigateTo(TransactionListRoute(), widgetNavOptions)
                "analytics"      -> appNavigator.navigateTo(AnalyticsRoute, widgetNavOptions)
                "exchange_rates" -> appNavigator.navigateTo(ExchangeRatesRoute, widgetNavOptions)
            }
            onWidgetRouteHandled()
        }
    }

    val currentProfile by vms.mainViewModel.currentProfile.collectAsStateWithLifecycle()
    val nonArchivedAccounts by vms.mainViewModel.nonArchivedAccounts.collectAsStateWithLifecycle()
    val starredTemplates by vms.mainViewModel.starredTemplates.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    // Swipe-to-open only on top-level destinations — on sub-screens the left-edge
    // swipe is reserved for predictive-back and opening the drawer there is disorienting.
    val isTopLevelDestination = when {
        currentDest?.hasRoute(DashboardRoute::class) == true          -> true
        currentDest?.hasRoute(AnalyticsRoute::class) == true          -> true
        currentDest?.hasRoute(ExchangeRatesRoute::class) == true      -> true
        currentDest?.hasRoute(TransactionListRoute::class) == true    ->
            navBackStackEntry?.toRoute<TransactionListRoute>()?.topLevel == true
        else                                                           -> false
    }

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
        // Routes through AppNavigator too — a back-press landing mid-transition is the same
        // RESUMED-gate hazard as a rapid drawer tap, not just a UI-trigger-specific quirk.
        appNavigator.navigateTo(DashboardRoute())
    }

    ModalNavigationDrawer(
            drawerState = drawerState,
            // `&& !isAnimationRunning` is what makes the first tap after opening the drawer
            // actually register. ModalNavigationDrawer puts an `anchoredDraggable` on its whole
            // container (NavigationDrawer.kt), and `anchoredDraggable` defaults
            // `startDragImmediately = state.isAnimationRunning` (AnchoredDraggable.kt) — meaning
            // that while the open animation is still settling, the drag claims the pointer on the
            // DOWN event without waiting for touch slop, so it can "catch" the moving drawer. That
            // consumes the tap before the NavigationDrawerItem's clickable ever completes one,
            // which is why the first tap did nothing until the user tapped again or waited for the
            // animation to finish. Disabling gestures only while animating gives the tap back; the
            // only thing lost is the ability to re-grab the drawer mid-animation.
            gesturesEnabled = isTopLevelDestination && !drawerState.isAnimationRunning,
            drawerContent = {
                AppDrawerContent(
                    drawerHazeState = drawerHazeState,
                    currentProfile = currentProfile,
                    nonArchivedAccounts = nonArchivedAccounts,
                    starredTemplates = starredTemplates,
                    selectedDestination = selectedRoute,
                    onDashboardClick = { navigateFromDrawer(DashboardRoute(topLevel = true)) },
                    onRecordsClick = { navigateFromDrawer(TransactionListRoute(topLevel = true)) },
                    onAnalyticsClick = { navigateFromDrawer(AnalyticsRoute) },
                    onExchangeRatesClick = { navigateFromDrawer(ExchangeRatesRoute) },
                    onSettingsClick = { navigateFromDrawer(SettingsRoute) },
                    onAccountClick = { id -> navigateFromDrawer(AccountDetailRoute(accountId = id)) },
                    onTemplateClick = { template ->
                        navigateFromDrawer(TransactionEntryRoute(templateId = template.id))
                    },
                    onPlannedPaymentsClick = { navigateFromDrawer(PlannedPaymentListRoute) },
                    onBudgetsClick = { navigateFromDrawer(BudgetListRoute) },
                    onLoansClick = { navigateFromDrawer(LoanListRoute) },
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().aeonHazeSource(drawerHazeState)) {
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
                            actions = OnboardingActions(
                                onComplete = {
                                    navController.navigate(DashboardRoute()) {
                                        popUpTo<OnboardingRoute> { inclusive = true }
                                    }
                                }
                            )
                        )
                    }

                    primaryTabsGraph(navController, drawerState, scope, vms)
                    settingsGraph(navController, vms)
                    accountsGraph(navController, vms)
                    transactionsGraph(navController, vms)
                    planningGraph(navController, vms)
                }
            }

            // Registered AFTER NavHost so it has higher LIFO priority than NavHost's internal
            // PredictiveBackHandler — ensures back press closes the drawer before popping routes.
            BackHandler(enabled = drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }
        }
}
