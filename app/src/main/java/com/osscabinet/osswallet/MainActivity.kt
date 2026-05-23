package com.osscabinet.ossWallet

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.navigation.OssWalletNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.osscabinet.ossWallet.ui.navigation.Screen
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.screens.main.MainViewModel
import com.osscabinet.ossWallet.ui.components.AppDrawerContent
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.navigation.navigateToRecords
import com.osscabinet.ossWallet.ui.navigation.navigateToDashboard
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import android.content.Intent
import kotlinx.coroutines.launch
import com.osscabinet.ossWallet.ui.theme.AppColors

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var keepSplashOnScreen = true

    @Inject lateinit var settingsRepository: SettingsRepository

    companion object {
        const val EXTRA_WIDGET_NAV = "WIDGET_NAV"
    }

    private var pendingWidgetRoute by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetRoute = intent.getStringExtra(EXTRA_WIDGET_NAV)
    }

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingWidgetRoute = intent?.getStringExtra(EXTRA_WIDGET_NAV)

        setContent {
            val theme by settingsRepository.theme.collectAsStateWithLifecycle(initialValue = "System")
            val selectedFont by settingsRepository.selectedFont.collectAsStateWithLifecycle(initialValue = "MANROPE")
            val precision by settingsRepository.decimalPrecision.collectAsStateWithLifecycle(initialValue = 2)
            val use24Hour by settingsRepository.use24HourFormat.collectAsStateWithLifecycle(initialValue = false)

            LaunchedEffect(precision) {
                com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.precision = precision
                com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision = precision
                com.osscabinet.ossWallet.domain.service.timeline.TimelineService.precision = precision
            }

            LaunchedEffect(use24Hour) {
                com.osscabinet.ossWallet.ui.util.TimeUtils.use24HourFormat = use24Hour
            }

            OssWalletTheme(theme = theme, fontName = selectedFont) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.screenBackground
                ) {
                    val navController = rememberNavController()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    var isDrawerNavigating by remember { mutableStateOf(false) }

                    BackHandler(enabled = drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    }
                    
                    val currentProfile by mainViewModel.currentProfile.collectAsStateWithLifecycle()
                    val nonArchivedAccounts by mainViewModel.nonArchivedAccounts.collectAsStateWithLifecycle()
                    val starredTemplates by mainViewModel.starredTemplates.collectAsStateWithLifecycle()
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    
                    LaunchedEffect(pendingWidgetRoute) {
                        pendingWidgetRoute?.let { route ->
                            when (route) {
                                "dashboard" -> navController.navigateToDashboard(DashboardViewContext.None, topLevel = true)
                                "records" -> navController.navigateToRecords(TransactionFilter(), topLevel = true)
                                "analytics" -> navController.navigate(Screen.Analytics.createRoute(topLevel = true))
                                "exchange_rates" -> navController.navigate(Screen.ExchangeRates.createRoute(topLevel = true))
                            }
                            pendingWidgetRoute = null
                        }
                    }

                    // Extract base route from currentDestination to match drawer highlights
                    val currentDestination = currentBackStackEntry?.destination?.route?.substringBefore("?") 
                        ?: Screen.Dashboard.route.substringBefore("?")

                    val isGesturesEnabled = !isDrawerNavigating && currentDestination != Screen.Onboarding.route

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = isGesturesEnabled,
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
                                        filter = TransactionFilter(),
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
                                    navController.navigate(Screen.TransactionEntry.createRoute(type = template.transactionType, templateId = template.id))
                                },
                                onPlannedPaymentsClick = {
                                    navController.navigate(Screen.PlannedPaymentList.route)
                                },
                                onBudgetsClick = {
                                    navController.navigate(Screen.BudgetList.route)
                                },
                                onLoansClick = {
                                    navController.navigate(Screen.LoanList.route)
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
                        OssWalletNavGraph(
                            navController = navController,
                            onSplashConditionChanged = { keepSplashOnScreen = it },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
    }
}
