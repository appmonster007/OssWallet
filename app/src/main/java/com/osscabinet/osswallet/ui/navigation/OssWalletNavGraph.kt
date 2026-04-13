package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    splashScreen: androidx.core.splashscreen.SplashScreen? = null
) {
    // Shared ViewModels or persistent ones can be initialized here or at activity level
    // For SOLID, we use Factories that use the container
    
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            appContainer.settingsDataStore,
            appContainer.profileRepository,
            appContainer.backupRepository
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
            appContainer.settingsDataStore,
            appContainer.db.categoryDao(),
            appContainer.accountRepository,
            analyticsViewModel
        )
    )
    
    val accountViewModel: AccountViewModel = viewModel(
        factory = AccountViewModelFactory(
            appContainer.accountRepository,
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

    val tagViewModel: TagViewModel = viewModel(
        factory = GenericViewModelFactory {
            TagViewModel(appContainer.tagRepository)
        }
    )

    // Keep splash screen on until Dashboard is loaded
    splashScreen?.setKeepOnScreenCondition {
        dashboardViewModel.uiState.value is DashboardUiState.Loading
    }

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
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
                templateViewModel = templateViewModel
            )
        }

        composable(Screen.ExchangeRates.route) {
            ExchangeRatesScreen(
                viewModel = currencyViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onManageAccountsClick = { navController.navigate(Screen.AccountManagement.route) },
                onManageLabelsClick = { navController.navigate(Screen.ManageLabels.route) },
                onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.ManageLabels.route) {
            ManageLabelsScreen(
                viewModel = tagViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Templates.route) {
            TemplatesScreen(
                viewModel = templateViewModel,
                accountViewModel = accountViewModel,
                onAddTemplateClick = { templateId -> 
                    navController.navigate(Screen.AddTemplate.createRoute(templateId))
                },
                onBackClick = { navController.popBackStack() }
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
                tagViewModel = tagViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Loans.route) {
            LoansScreen(
                viewModel = loanViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Budgets.route) {
            BudgetsScreen(
                viewModel = budgetViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onEditBudgetClick = { category -> navController.navigate(Screen.EditBudget.createRoute(category)) }
            )
        }

        composable(Screen.EditBudget.route) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            EditBudgetScreen(
                category = category,
                viewModel = budgetViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.TransactionList.route) {
            TransactionListScreen(
                viewModel = transactionViewModel,
                accountViewModel = accountViewModel,
                onAddTransactionClick = { navController.navigate(Screen.AddTransaction.route) },
                onEditTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                onBackClick = { navController.popBackStack() },
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
            AddTransactionScreen(
                accountId = accountId,
                initialType = type,
                initialTemplate = template,
                viewModel = transactionViewModel,
                accountViewModel = accountViewModel,
                analyticsViewModel = analyticsViewModel,
                templateViewModel = templateViewModel,
                tagViewModel = tagViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.EditTransaction.route) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull() ?: -1L
            EditTransactionScreen(
                transactionId = transactionId,
                viewModel = transactionViewModel,
                accountViewModel = accountViewModel,
                analyticsViewModel = analyticsViewModel,
                settingsViewModel = settingsViewModel,
                tagViewModel = tagViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.AccountDetail.route) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            AccountDetailScreen(
                accountId = accountId,
                accountViewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                settingsViewModel = settingsViewModel,
                analyticsViewModel = analyticsViewModel,
                onBackClick = { navController.popBackStack() },
                onEditAccountClick = { id -> navController.navigate(Screen.EditAccount.createRoute(id)) },
                onEditTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                onAddTransactionClick = { id -> navController.navigate(Screen.AddTransaction.createRoute(id)) }
            )
        }

        composable(Screen.EditAccount.route) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            EditAccountScreen(
                accountId = accountId,
                viewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.AccountSetup.route) {
            AccountSetupScreen(
                onBackClick = { navController.popBackStack() },
                onAccountCreated = { navController.popBackStack() },
                viewModel = accountViewModel
            )
        }

        composable(Screen.AccountManagement.route) {
            AccountManagementScreen(
                onBackClick = { navController.popBackStack() },
                onAddAccountClick = { navController.navigate(Screen.AccountSetup.route) },
                onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                viewModel = accountViewModel
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.SharedWallet.route) {
            SharedWalletScreen(
                viewModel = sharedWalletViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
