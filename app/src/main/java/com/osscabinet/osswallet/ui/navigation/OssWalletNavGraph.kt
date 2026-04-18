package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.osscabinet.ossWallet.di.AppContainer
import com.osscabinet.ossWallet.ui.screens.*
import com.osscabinet.ossWallet.ui.viewmodel.*

@Composable
fun OssWalletNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    onSplashConditionChanged: (Boolean) -> Unit = {}
) {
    // Shared ViewModels or persistent ones can be initialized here or at activity level
    // For SOLID, we use Factories that use the container
    
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
                appContainer.getTransactionsUseCase,
                appContainer.getExchangeRatesUseCase,
                appContainer.settingsDataStore
            )
        }
    )
    
    val transactionViewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModelFactory(
            appContainer.getTransactionsUseCase,
            appContainer.insertTransactionUseCase,
            appContainer.deleteTransactionUseCase,
            appContainer.updateTransactionUseCase,
            appContainer.saveTransferUseCase,
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
            appContainer.syncBudgetSpentUseCase,
            appContainer.upsertBudgetUseCase
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

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = GenericViewModelFactory {
            DashboardViewModel(
                appContainer.accountRepository,
                appContainer.getTransactionsUseCase,
                appContainer.settingsDataStore,
                analyticsViewModel
            )
        }
    )

    val accountsViewViewModel: AccountsViewViewModel = viewModel(
        factory = GenericViewModelFactory {
            AccountsViewViewModel(
                appContainer.accountRepository,
                appContainer.getTransactionsUseCase,
                appContainer.settingsDataStore,
                analyticsViewModel
            )
        }
    )

    val tagViewModel: TagViewModel = viewModel(
        factory = GenericViewModelFactory {
            TagViewModel(appContainer.tagRepository)
        }
    )

    val isOnboardingCompleted by settingsViewModel.isOnboardingCompleted.collectAsState()
    val dashboardUiState by dashboardViewModel.uiState.collectAsState()

    val keepSplashOnScreen = when (isOnboardingCompleted) {
        null -> true
        false -> false
        true -> dashboardUiState is DashboardUiState.Loading
    }

    SideEffect {
        onSplashConditionChanged(keepSplashOnScreen)
    }

    // Safe popBackStack wrapper
    val safePopBackStack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    if (isOnboardingCompleted == null) {
        // Show a loading screen or keep splash screen instead of returning nothing
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
            DashboardScreen(
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
                budgetViewModel = budgetViewModel
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
                accountsViewViewModel = accountsViewViewModel
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
            val filters by settingsViewModel.allFilters.collectAsState(initial = emptyList())
            val filterEntity = filters.find { it.id == filterId }
            
            val accounts by accountViewModel.accounts.collectAsState()
            val allCategories by transactionViewModel.categories.collectAsState()

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
                        getTransactionsUseCase = appContainer.getTransactionsUseCase,
                        settingsDataStore = appContainer.settingsDataStore,
                        analyticsViewModel = analyticsViewModel
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
