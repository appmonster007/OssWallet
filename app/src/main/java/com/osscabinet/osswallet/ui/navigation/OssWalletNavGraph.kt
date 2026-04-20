package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.osscabinet.ossWallet.di.AppContainer
import com.osscabinet.ossWallet.ui.dashboard.DashboardScreen
import com.osscabinet.ossWallet.ui.dashboard.DashboardViewModel
import com.osscabinet.ossWallet.ui.dashboard.DashboardUiState
import com.osscabinet.ossWallet.ui.dashboard.DashboardSection
import com.osscabinet.ossWallet.ui.accountdetail.AccountDetailScreen
import com.osscabinet.ossWallet.ui.viewmodel.AccountDetailViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountDetailUiState
import com.osscabinet.ossWallet.ui.screens.OnboardingScreen
import com.osscabinet.ossWallet.ui.screens.AccountsViewScreen
import com.osscabinet.ossWallet.ui.screens.ExchangeRatesScreen
import com.osscabinet.ossWallet.ui.screens.SettingsScreen
import com.osscabinet.ossWallet.ui.screens.FiltersSettingsScreen
import com.osscabinet.ossWallet.ui.screens.EditFilterScreen
import com.osscabinet.ossWallet.ui.screens.ManageLabelsScreen
import com.osscabinet.ossWallet.ui.screens.CategoryManagementScreen
import com.osscabinet.ossWallet.ui.screens.TemplatesScreen
import com.osscabinet.ossWallet.ui.screens.AddTemplateScreen
import com.osscabinet.ossWallet.ui.screens.LoansScreen
import com.osscabinet.ossWallet.ui.screens.BudgetsScreen
import com.osscabinet.ossWallet.ui.screens.EditBudgetScreen
import com.osscabinet.ossWallet.ui.screens.TransactionListScreen
import com.osscabinet.ossWallet.ui.screens.TransactionEntryScreen
import com.osscabinet.ossWallet.ui.screens.EditAccountScreen
import com.osscabinet.ossWallet.ui.screens.AccountSetupScreen
import com.osscabinet.ossWallet.ui.screens.AccountManagementScreen
import com.osscabinet.ossWallet.ui.screens.AnalyticsScreen
import com.osscabinet.ossWallet.ui.screens.SharedWalletScreen
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModelFactory
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModelFactory
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModelFactory
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModelFactory
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SharedWalletViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import com.osscabinet.ossWallet.ui.viewmodel.CurrencyViewModel
import com.osscabinet.ossWallet.ui.viewmodel.GenericViewModelFactory
import com.osscabinet.ossWallet.ui.viewmodel.AccountsViewViewModel
import com.osscabinet.ossWallet.ui.viewmodel.CardManagerViewModel
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModelFactory
import com.osscabinet.ossWallet.ui.viewmodel.TagViewModel

@Composable
fun OssWalletNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    onSplashConditionChanged: (Boolean) -> Unit = {}
) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            appContainer.settingsDataStore,
            appContainer.profileRepository,
            appContainer.backupRepository,
            appContainer.currencyRepository,
            appContainer.db.filterDao()
        )
    )

    val analyticsViewModel: AnalyticsViewModel = viewModel(
        factory = GenericViewModelFactory {
            AnalyticsViewModel(
                appContainer.transactionUseCases,
                appContainer.financialUseCases,
                appContainer.settingsDataStore
            )
        }
    )
    
    val transactionViewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModelFactory(
            appContainer.transactionUseCases,
            appContainer.settingsDataStore,
            appContainer.db.categoryDao(),
            appContainer.accountRepository,
            appContainer.db.filterDao(),
            analyticsViewModel
        )
    )
    
    val accountViewModel: AccountViewModel = viewModel(
        factory = AccountViewModelFactory(
            appContainer.accountRepository,
            appContainer.transactionRepository,
            appContainer.settingsDataStore
        )
    )

    val budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModelFactory(
            appContainer.budgetUseCases
        )
    )

    val loanViewModel: LoanViewModel = viewModel(
        factory = LoanViewModelFactory(appContainer.loanRepository)
    )
    
    val sharedWalletViewModel: SharedWalletViewModel = viewModel(
        factory = GenericViewModelFactory {
            SharedWalletViewModel(appContainer.sharedWalletRepository)
        }
    )

    val templateViewModel: TemplateViewModel = viewModel(
        factory = GenericViewModelFactory {
            TemplateViewModel(appContainer.db.templateDao())
        }
    )

    val currencyViewModel: CurrencyViewModel = viewModel(
        factory = GenericViewModelFactory {
            CurrencyViewModel(appContainer.currencyRepository, appContainer.db.currencyDao(), appContainer.settingsDataStore)
        }
    )

    val cardManagerViewModel: CardManagerViewModel = viewModel(
        factory = GenericViewModelFactory {
            CardManagerViewModel(
                appContainer.settingsDataStore
            )
        }
    )

    val dashboardViewModel: com.osscabinet.ossWallet.ui.dashboard.DashboardViewModel = viewModel(
        factory = GenericViewModelFactory {
            com.osscabinet.ossWallet.ui.dashboard.DashboardViewModel(
                appContainer.accountRepository,
                appContainer.transactionUseCases,
                appContainer.settingsDataStore,
                analyticsViewModel,
                appContainer.financialUseCases,
                cardManagerViewModel
            )
        }
    )

    val accountsViewViewModel: AccountsViewViewModel = viewModel(
        factory = GenericViewModelFactory {
            AccountsViewViewModel(
                appContainer.accountRepository,
                appContainer.transactionUseCases,
                appContainer.settingsDataStore,
                analyticsViewModel,
                appContainer.financialUseCases
            )
        }
    )

    val tagViewModel: TagViewModel = viewModel(
        factory = GenericViewModelFactory {
            TagViewModel(appContainer.tagRepository)
        }
    )

    val isOnboardingCompleted by settingsViewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    val keepSplashOnScreen = when (isOnboardingCompleted) {
        null -> true
        false -> false
        true -> dashboardUiState is com.osscabinet.ossWallet.ui.dashboard.DashboardUiState.Loading
    }

    SideEffect {
        onSplashConditionChanged(keepSplashOnScreen)
    }

    val safePopBackStack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    if (isOnboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController, 
        startDestination = if (isOnboardingCompleted == true) Screen.Dashboard.route else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                viewModel = settingsViewModel,
                accountViewModel = accountViewModel,
                onFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            com.osscabinet.ossWallet.ui.dashboard.DashboardScreen(
                onAnalyticsClick = { navController.navigate(Screen.Analytics.route) },
                onRecordsClick = { navController.navigate(Screen.TransactionList.route) },
                onBudgetsClick = { navController.navigate(Screen.Budgets.route) },
                onAddClick = { type, template -> 
                    navController.navigate(Screen.AddTransaction.createRoute(type = type, template = template)) 
                },
                onManageAccountsClick = { navController.navigate(Screen.AccountManagement.route) },
                onAccountClick = { accountId -> navController.navigate(Screen.AccountDetail.createRoute(accountId)) },
                onLoansClick = { navController.navigate(Screen.Loans.route) },
                onExchangeRatesClick = { navController.navigate(Screen.ExchangeRates.route) },
                onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onAccountsViewClick = { navController.navigate(Screen.AccountsView.route) },
                accountViewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                settingsViewModel = settingsViewModel,
                analyticsViewModel = analyticsViewModel,
                templateViewModel = templateViewModel,
                dashboardViewModel = dashboardViewModel,
                budgetViewModel = budgetViewModel,
                cardManagerViewModel = cardManagerViewModel
            )
        }
        
        composable(Screen.AccountsView.route) {
            AccountsViewScreen(
                onDashboardClick = { 
                    navController.navigate(Screen.Dashboard.route) { 
                        popUpTo(Screen.Dashboard.route) { inclusive = true } 
                    } 
                },
                onRecordsClick = { navController.navigate(Screen.TransactionList.route) },
                onAnalyticsClick = { navController.navigate(Screen.Analytics.route) },
                onBudgetsClick = { navController.navigate(Screen.Budgets.route) },
                onLoansClick = { navController.navigate(Screen.Loans.route) },
                onExchangeRatesClick = { navController.navigate(Screen.ExchangeRates.route) },
                onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                onManageAccountsClick = { navController.navigate(Screen.AccountManagement.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onAccountClick = { accountId -> navController.navigate(Screen.AccountDetail.createRoute(accountId)) },
                onAddAccountClick = { navController.navigate(Screen.AccountSetup.route) },
                onAddClick = { type, template -> 
                    navController.navigate(Screen.AddTransaction.createRoute(type = type, template = template)) 
                },
                accountViewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                settingsViewModel = settingsViewModel,
                analyticsViewModel = analyticsViewModel,
                templateViewModel = templateViewModel,
                accountsViewViewModel = accountsViewViewModel,
                cardManagerViewModel = cardManagerViewModel
            )
        }

        composable(Screen.ExchangeRates.route) {
            ExchangeRatesScreen(
                viewModel = currencyViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onManageAccountsClick = { navController.navigate(Screen.AccountManagement.route) },
                onManageLabelsClick = { navController.navigate(Screen.ManageLabels.route) },
                onManageCategoriesClick = { navController.navigate(Screen.ManageCategories.route) },
                onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                onManageFiltersClick = { navController.navigate(Screen.ManageFilters.route) },
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.ManageFilters.route) {
            FiltersSettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = safePopBackStack,
                onAddFilterClick = { navController.navigate(Screen.EditFilter.route) },
                onEditFilterClick = { filter -> 
                    navController.navigate(Screen.EditFilter.createRoute(filter.id))
                }
            )
        }

        composable(
            Screen.EditFilter.route,
            arguments = listOf(
                navArgument("filterId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val filterId = backStackEntry.arguments?.getLong("filterId").takeIf { it != -1L }
            val filters by settingsViewModel.allFilters.collectAsStateWithLifecycle(initialValue = emptyList())
            val filterEntity = filters.find { it.id == filterId }
            
            val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
            val allCategories by transactionViewModel.categories.collectAsStateWithLifecycle()

            EditFilterScreen(
                filterEntity = filterEntity,
                accounts = accounts,
                allCategories = allCategories,
                onSaveFilter = { 
                    settingsViewModel.saveFilter(it)
                    safePopBackStack()
                },
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.ManageLabels.route) {
            ManageLabelsScreen(
                viewModel = tagViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.ManageCategories.route) {
            CategoryManagementScreen(
                viewModel = transactionViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.Templates.route) {
            TemplatesScreen(
                viewModel = templateViewModel,
                accountViewModel = accountViewModel,
                onAddTemplateClick = { templateId -> 
                    navController.navigate(Screen.AddTemplate.createRoute(templateId))
                },
                onBackClick = safePopBackStack
            )
        }

        composable(
            Screen.AddTemplate.route,
            arguments = listOf(
                navArgument("templateId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong("templateId").takeIf { it != -1L }
            AddTemplateScreen(
                templateId = templateId,
                templateViewModel = templateViewModel,
                accountViewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                tagViewModel = tagViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.Loans.route) {
            LoansScreen(
                viewModel = loanViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.Budgets.route) {
            BudgetsScreen(
                viewModel = budgetViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = safePopBackStack,
                onEditBudgetClick = { category -> navController.navigate(Screen.EditBudget.createRoute(category)) }
            )
        }

        composable(Screen.EditBudget.route) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            EditBudgetScreen(
                category = category,
                viewModel = budgetViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.TransactionList.route) {
            TransactionListScreen(
                viewModel = transactionViewModel,
                accountViewModel = accountViewModel,
                analyticsViewModel = analyticsViewModel,
                templateViewModel = templateViewModel,
                onAddTransactionClick = { type, name -> 
                    navController.navigate(Screen.AddTransaction.createRoute(type = type, template = name))
                },
                onEditTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                onBackClick = safePopBackStack,
                onAccountDetailClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) }
            )
        }

        composable(
            Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("accountId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("type") {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument("template") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")
            val type = backStackEntry.arguments?.getInt("type") ?: 0
            val template = backStackEntry.arguments?.getString("template")
            TransactionEntryScreen(
                accountId = accountId,
                initialType = type,
                initialTemplate = template,
                viewModel = transactionViewModel,
                accountViewModel = accountViewModel,
                analyticsViewModel = analyticsViewModel,
                templateViewModel = templateViewModel,
                tagViewModel = tagViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.EditTransaction.route) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull() ?: -1L
            TransactionEntryScreen(
                transactionId = transactionId,
                viewModel = transactionViewModel,
                accountViewModel = accountViewModel,
                analyticsViewModel = analyticsViewModel,
                templateViewModel = templateViewModel,
                tagViewModel = tagViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.AccountDetail.route) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            val accountDetailViewModel: AccountDetailViewModel = viewModel(
                key = "AccountDetailViewModel_$accountId",
                factory = GenericViewModelFactory {
                    AccountDetailViewModel(
                        accountId = accountId,
                        accountRepository = appContainer.accountRepository,
                        transactionUseCases = appContainer.transactionUseCases,
                        settingsDataStore = appContainer.settingsDataStore,
                        analyticsViewModel = analyticsViewModel,
                        financialUseCases = appContainer.financialUseCases
                    )
                }
            )
            AccountDetailScreen(
                accountId = accountId,
                accountViewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                settingsViewModel = settingsViewModel,
                analyticsViewModel = analyticsViewModel,
                accountDetailViewModel = accountDetailViewModel,
                cardManagerViewModel = cardManagerViewModel,
                templateViewModel = templateViewModel,
                onBackClick = safePopBackStack,
                onEditAccountClick = { id -> navController.navigate(Screen.EditAccount.createRoute(id)) },
                onEditTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                onAddTransactionClick = { accId, type, name -> 
                    navController.navigate(Screen.AddTransaction.createRoute(accountId = accId, type = type, template = name))
                }
            )
        }

        composable(Screen.EditAccount.route) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            EditAccountScreen(
                accountId = accountId,
                viewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.AccountSetup.route) {
            AccountSetupScreen(
                onBackClick = safePopBackStack,
                onAccountCreated = safePopBackStack,
                viewModel = accountViewModel
            )
        }

        composable(Screen.AccountManagement.route) {
            AccountManagementScreen(
                onBackClick = safePopBackStack,
                onAddAccountClick = { navController.navigate(Screen.AccountSetup.route) },
                onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                viewModel = accountViewModel
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.SharedWallet.route) {
            SharedWalletScreen(
                viewModel = sharedWalletViewModel,
                onBackClick = safePopBackStack
            )
        }
    }
}
