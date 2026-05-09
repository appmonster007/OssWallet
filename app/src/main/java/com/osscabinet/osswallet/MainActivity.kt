package com.osscabinet.ossWallet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.osscabinet.ossWallet.di.AppContainer
import com.osscabinet.ossWallet.ui.navigation.OssWalletNavGraph
import com.osscabinet.ossWallet.ui.navigation.Screen
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.viewmodel.MainViewModel
import com.osscabinet.ossWallet.ui.viewmodel.GenericViewModelFactory
import com.osscabinet.ossWallet.ui.components.AppDrawerContent
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import com.osscabinet.ossWallet.ui.navigation.navigateToRecords
import com.osscabinet.ossWallet.ui.navigation.navigateToDashboard
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import android.content.Intent
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var keepSplashOnScreen = true
    private lateinit var appContainer: AppContainer

    companion object {
        const val EXTRA_WIDGET_NAV = "WIDGET_NAV"
    }

    private var pendingWidgetRoute by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetRoute = intent.getStringExtra(EXTRA_WIDGET_NAV)
    }

    private val mainViewModel: MainViewModel by viewModels {
        GenericViewModelFactory {
            MainViewModel(
                appContainer.profileRepository,
                appContainer.settingsRepository,
                appContainer.accountRepository,
                appContainer.templateRepository
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize dependency container
        appContainer = (application as OssWalletApp).container

        pendingWidgetRoute = intent?.getStringExtra(EXTRA_WIDGET_NAV)

        setContent {
            val theme by appContainer.settingsDataStore.theme.collectAsStateWithLifecycle(initialValue = "System")

            OssWalletTheme(theme = theme) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                val currentProfile by mainViewModel.currentProfile.collectAsStateWithLifecycle()
                val nonArchivedAccounts by mainViewModel.nonArchivedAccounts.collectAsStateWithLifecycle()
                val starredTemplates by mainViewModel.starredTemplates.collectAsStateWithLifecycle()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                
                LaunchedEffect(pendingWidgetRoute) {
                    pendingWidgetRoute?.let { route ->
                        when (route) {
                            "dashboard" -> navController.navigateToDashboard(DashboardViewContext.None, topLevel = true)
                            "records" -> navController.navigateToRecords(RecordsFilterContext.None, topLevel = true)
                            "analytics" -> navController.navigate(Screen.Analytics.createRoute(topLevel = true))
                            "exchange_rates" -> navController.navigate(Screen.ExchangeRates.createRoute(topLevel = true))
                        }
                        pendingWidgetRoute = null
                    }
                }

                // Extract base route from currentDestination to match drawer highlights
                val currentDestination = currentBackStackEntry?.destination?.route?.substringBefore("?") 
                    ?: Screen.Dashboard.route.substringBefore("?")

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawerContent(
                            currentProfile = currentProfile,
                            nonArchivedAccounts = nonArchivedAccounts,
                            starredTemplates = starredTemplates,
                            selectedDestination = currentDestination,
                            onDashboardClick = {
                                navController.navigateToDashboard(
                                    context = DashboardViewContext.None,
                                    topLevel = true
                                ) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onRecordsClick = {
                                navController.navigateToRecords(
                                    context = RecordsFilterContext.None,
                                    topLevel = true
                                ) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onAnalyticsClick = {
                                navController.navigate(Screen.Analytics.createRoute(topLevel = true)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onExchangeRatesClick = {
                                navController.navigate(Screen.ExchangeRates.createRoute(topLevel = true)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onSettingsClick = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onAccountClick = { id ->
                                navController.navigate(Screen.AccountDetail.createRoute(id))
                            },
                            onTemplateClick = { template ->
                                navController.navigate(Screen.AddTransaction.createRoute(type = template.transactionType, template = template.name))
                            },
                            onCloseDrawer = { scope.launch { drawerState.close() } },
                            onPlannedPaymentsClick = {
                                navController.navigate(Screen.PlannedPayments.route)
                            },
                            onBudgetsClick = {
                                navController.navigate(Screen.BudgetList.route)
                            },
                            onLoansClick = {
                                navController.navigate(Screen.LoanList.route)
                            }
                        )
                    }
                ) {
                    OssWalletNavGraph(
                        navController = navController,
                        appContainer = appContainer,
                        onSplashConditionChanged = { keepSplashOnScreen = it },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}
