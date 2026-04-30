package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.di.AppContainer
import com.osscabinet.ossWallet.ui.dashboard.*
import com.osscabinet.ossWallet.ui.viewmodel.*
import com.osscabinet.ossWallet.ui.screens.*
import com.osscabinet.ossWallet.ui.analytics.AnalyticsScreen
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryScreen
import com.osscabinet.ossWallet.ui.accountdetail.*
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun OssWalletNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    onSplashConditionChanged: (Boolean) -> Unit = {}
) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            appContainer.settingsRepository,
            appContainer.profileRepository,
            appContainer.backupRepository,
            appContainer.currencyRepository,
            appContainer.filterRepository
        )
    )

    val analyticsViewModel: AnalyticsViewModel = viewModel(
        factory = GenericViewModelFactory {
            AnalyticsViewModel(
                appContainer.transactionUseCases,
                appContainer.settingsRepository,
                appContainer.computationRepository,
                appContainer.categoryRepository,
                appContainer.accountRepository
            )
        }
    )
    
    val transactionViewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModelFactory(
            appContainer.transactionUseCases,
            appContainer.settingsRepository,
            appContainer.categoryRepository,
            appContainer.accountRepository,
            appContainer.filterRepository,
            appContainer.computationRepository,
            appContainer.transactionManager
        )
    )
    
    val accountViewModel: AccountViewModel = viewModel(
        factory = AccountViewModelFactory(
            appContainer.accountRepository,
            appContainer.transactionRepository,
            appContainer.settingsRepository
        )
    )

    val sharedWalletViewModel: SharedWalletViewModel = viewModel(
        factory = GenericViewModelFactory {
            SharedWalletViewModel(appContainer.sharedWalletRepository)
        }
    )

    val templateViewModel: TemplateViewModel = viewModel(
        factory = GenericViewModelFactory {
            TemplateViewModel(appContainer.templateRepository)
        }
    )

    val currencyViewModel: CurrencyViewModel = viewModel(
        factory = GenericViewModelFactory {
            CurrencyViewModel(appContainer.currencyRepository, appContainer.settingsRepository)
        }
    )

    val cardLayoutViewModel: CardLayoutViewModel = viewModel(
        factory = GenericViewModelFactory {
            CardLayoutViewModel(appContainer.cardRepository)
        }
    )

    val cardConfigViewModel: CardConfigViewModel = viewModel(
        factory = GenericViewModelFactory {
            CardConfigViewModel(appContainer.cardRepository)
        }
    )

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = GenericViewModelFactory {
            DashboardViewModel(
                appContainer.accountRepository,
                appContainer.transactionUseCases,
                appContainer.settingsRepository,
                appContainer.computationRepository,
                appContainer.cardRepository,
                appContainer.categoryRepository,
                appContainer.cardComputerRegistry
            )
        }
    )

    val accountsViewViewModel: AccountsViewViewModel = viewModel(
        factory = GenericViewModelFactory {
            AccountsViewViewModel(
                appContainer.accountRepository,
                appContainer.transactionUseCases,
                appContainer.settingsRepository,
                appContainer.cardRepository,
                appContainer.categoryRepository,
                appContainer.computationRepository,
                appContainer.cardComputerRegistry
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
        true -> dashboardUiState is DashboardUiState.Loading
    }

    SideEffect {
        onSplashConditionChanged(keepSplashOnScreen)
    }

    if (isOnboardingCompleted == false) {
        OnboardingScreen(
            viewModel = settingsViewModel,
            accountViewModel = accountViewModel,
            onFinished = { /* State change will trigger re-composition */ }
        )
    } else if (isOnboardingCompleted == true) {
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route
        ) {
            composable(
                route = Screen.Dashboard.route,
                arguments = listOf(navArgument("viewContext") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val viewContext = backStackEntry.arguments?.getString("viewContext")
                DashboardScreen(
                    onAnalyticsClick = { navController.navigate(Screen.Analytics.route) },
                    onRecordsClick = { context -> navController.navigateToRecords(context) },
                    onAddClick = { type, template -> 
                        navController.navigate(Screen.AddTransaction.createRoute(type = type, template = template)) 
                    },
                    onManageAccountsClick = { navController.navigate(Screen.AccountManagement.route) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                    onAccountsViewClick = { navController.navigate(Screen.AccountsView.route) },
                    onExchangeRatesClick = { navController.navigate(Screen.ExchangeRates.route) },
                    onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onEditTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onCloneTransactionClick = { id ->
                        transactionViewModel.setCloneTransactionId(id)
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    settingsViewModel = settingsViewModel,
                    analyticsViewModel = analyticsViewModel,
                    templateViewModel = templateViewModel,
                    dashboardViewModel = dashboardViewModel,
                    cardLayoutViewModel = cardLayoutViewModel,
                    cardComputerRegistry = appContainer.cardComputerRegistry,
                    viewContext = viewContext
                )
            }

            composable(
                route = Screen.AccountsView.route,
                arguments = listOf(navArgument("filterContext") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val filterContext = backStackEntry.arguments?.getString("filterContext")
                AccountsViewScreen(
                    onDashboardClick = { navController.popBackStack() },
                    onRecordsClick = { context -> navController.navigateToRecords(context) },
                    onAnalyticsClick = { navController.navigate(Screen.Analytics.route) },
                    onExchangeRatesClick = { navController.navigate(Screen.ExchangeRates.route) },
                    onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                    onManageAccountsClick = { navController.navigate(Screen.AccountManagement.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
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
                    cardLayoutViewModel = cardLayoutViewModel,
                    cardComputerRegistry = appContainer.cardComputerRegistry,
                    filterContext = filterContext
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
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TransactionList.route,
                arguments = listOf(navArgument("filterContext") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val filterContext = backStackEntry.arguments?.getString("filterContext")
                TransactionListScreen(
                    viewModel = transactionViewModel,
                    accountViewModel = accountViewModel,
                    analyticsViewModel = analyticsViewModel,
                    onAddTransactionClick = { type, template -> 
                        navController.navigate(Screen.AddTransaction.createRoute(type = type, template = template)) 
                    },
                    onEditTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onCloneTransactionClick = { id ->
                        transactionViewModel.setCloneTransactionId(id)
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    onBackClick = { navController.popBackStack() },
                    onAccountDetailClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                    templateViewModel = templateViewModel,
                    filterContext = filterContext
                )
            }

            composable(
                route = Screen.AddTransaction.route + "?accountId={accountId}&type={type}&template={template}",
                arguments = listOf(
                    navArgument("accountId") { type = NavType.StringType; nullable = true },
                    navArgument("type") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("template") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId")
                val type = backStackEntry.arguments?.getInt("type") ?: 0
                val template = backStackEntry.arguments?.getString("template")
                val prefillDto by transactionViewModel.prefillDto.collectAsStateWithLifecycle()
                val entryViewModel: com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryViewModel(
                            savedStateHandle = androidx.lifecycle.SavedStateHandle(),
                            transactionUseCases = appContainer.transactionUseCases,
                            transactionManager = appContainer.transactionManager,
                            settingsRepository = appContainer.settingsRepository,
                            computationRepository = appContainer.computationRepository,
                            accountRepository = appContainer.accountRepository,
                            categoryRepository = appContainer.categoryRepository,
                            tagRepository = appContainer.tagRepository,
                            templateRepository = appContainer.templateRepository
                        )
                    }
                )
                TransactionEntryScreen(
                    accountId = accountId,
                    initialType = type,
                    initialTemplate = template,
                    prefillDto = prefillDto,
                    viewModel = entryViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    templateViewModel = templateViewModel,
                    settingsViewModel = settingsViewModel,
                    onBackClick = { navController.popBackStack() },
                    onAccountsViewClick = { navController.navigate(Screen.AccountsView.route) },
                    onRecordsClick = { context -> navController.navigateToRecords(context) },
                    onExchangeRatesClick = { navController.navigate(Screen.ExchangeRates.route) },
                    onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                    onManageAccountsClick = { navController.navigate(Screen.AccountManagement.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                    onAddClick = { type, template -> 
                        navController.navigate(Screen.AddTransaction.createRoute(type = type, template = template))
                    },
                    cardComputerRegistry = appContainer.cardComputerRegistry
                )
            }

            composable(Screen.SharedWallet.route) {
                SharedWalletScreen(
                    viewModel = sharedWalletViewModel,
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

            composable(Screen.ExchangeRates.route) {
                ExchangeRatesScreen(
                    viewModel = currencyViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Templates.route) {
                TemplatesScreen(
                    viewModel = templateViewModel,
                    accountViewModel = accountViewModel,
                    onAddTemplateClick = { id -> navController.navigate(Screen.AddTemplate.createRoute(id)) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddTemplate.route,
                arguments = listOf(navArgument("templateId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L
                AddTemplateScreen(
                    templateId = if (templateId == -1L) null else templateId,
                    templateViewModel = templateViewModel,
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    tagViewModel = tagViewModel,
                    settingsViewModel = settingsViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: -1L
                val entryViewModel: com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryViewModel(
                            savedStateHandle = androidx.lifecycle.SavedStateHandle(),
                            transactionUseCases = appContainer.transactionUseCases,
                            transactionManager = appContainer.transactionManager,
                            settingsRepository = appContainer.settingsRepository,
                            computationRepository = appContainer.computationRepository,
                            accountRepository = appContainer.accountRepository,
                            categoryRepository = appContainer.categoryRepository,
                            tagRepository = appContainer.tagRepository,
                            templateRepository = appContainer.templateRepository
                        )
                    }
                )
                TransactionEntryScreen(
                    transactionId = transactionId,
                    viewModel = entryViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AccountDetail.route,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
                val accountDetailViewModel: AccountDetailViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        AccountDetailViewModel(
                            accountId = accountId,
                            accountRepository = appContainer.accountRepository,
                            transactionUseCases = appContainer.transactionUseCases,
                            settingsRepository = appContainer.settingsRepository,
                            cardRepository = appContainer.cardRepository,
                            categoryRepository = appContainer.categoryRepository,
                            computationRepository = appContainer.computationRepository,
                            registry = appContainer.cardComputerRegistry
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
                    cardLayoutViewModel = cardLayoutViewModel,
                    cardComputerRegistry = appContainer.cardComputerRegistry,
                    templateViewModel = templateViewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditAccountClick = { id -> navController.navigate(Screen.EditAccount.createRoute(id)) },
                    onEditTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onCloneTransactionClick = { id ->
                        transactionViewModel.setCloneTransactionId(id)
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    onAddTransactionClick = { accId, type, template ->
                        navController.navigate(Screen.AddTransaction.createRoute(accountId = accId, type = type, template = template))
                    },
                    onRecordsClick = { context -> navController.navigateToRecords(context) }
                )
            }

            composable(
                route = Screen.EditAccount.route,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
                EditAccountScreen(
                    accountId = accountId,
                    viewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.ManageLabels.route) {
                ManageLabelsScreen(
                    viewModel = tagViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.ManageCategories.route) {
                CategoryManagementScreen(
                    viewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.ManageFilters.route) {
                val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
                val categories by transactionViewModel.categories.collectAsStateWithLifecycle()

                FiltersSettingsScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { navController.popBackStack() },
                    onAddFilterClick = { 
                        navController.navigate(Screen.EditFilter.route)
                    },
                    onEditFilterClick = { filter -> 
                        navController.navigate(Screen.EditFilter.createRoute(filter.id)) 
                    }
                )
            }

            composable(
                route = Screen.EditFilter.route,
                arguments = listOf(navArgument("filterId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val filterId = backStackEntry.arguments?.getLong("filterId") ?: -1L
                val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
                val filterEntity = remember(filterId, savedFilters) {
                    savedFilters.find { it.id == filterId }
                }
                val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
                val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
                
                EditFilterScreen(
                    filter = filterEntity,
                    accounts = accounts,
                    allCategories = categories,
                    onSaveFilter = { entity ->
                        settingsViewModel.saveFilter(entity)
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
