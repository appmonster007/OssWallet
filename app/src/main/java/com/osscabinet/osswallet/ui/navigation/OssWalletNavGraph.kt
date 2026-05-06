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
    onSplashConditionChanged: (Boolean) -> Unit = {},
    onOpenDrawer: () -> Unit = {}
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
                appContainer.accountRepository,
                appContainer.cardComputerRegistry,
                appContainer.cardRepository,
                appContainer.plannedPaymentRepository,
                appContainer.budgetRepository,
                appContainer.loanRepository
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

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = GenericViewModelFactory {
            DashboardViewModel(
                appContainer.accountRepository,
                appContainer.transactionUseCases,
                appContainer.settingsRepository,
                appContainer.computationRepository,
                appContainer.cardRepository,
                appContainer.categoryRepository,
                appContainer.cardComputerRegistry,
                appContainer.plannedPaymentRepository,
                appContainer.budgetRepository,
                appContainer.loanRepository,
                appContainer.transactionManager
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
            startDestination = Screen.Dashboard.createRoute(topLevel = true)
        ) {
            composable(
                route = Screen.Dashboard.route,
                arguments = listOf(
                    navArgument("viewContext") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("topLevel") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                ),
                enterTransition = { FadeEnterTransitionSpec },
                exitTransition = { FadeExitTransitionSpec },
                popEnterTransition = { FadeEnterTransitionSpec },
                popExitTransition = { FadeExitTransitionSpec }
            ) { backStackEntry ->
                val viewContext = backStackEntry.arguments?.getString("viewContext")
                val topLevel = backStackEntry.arguments?.getBoolean("topLevel") ?: false
                DashboardScreen(
                    onMenuClick = onOpenDrawer,
                    onBackClick = { navController.popBackStack() },
                    isTopLevelParam = topLevel,
                    onRecordsClick = { context -> navController.navigateToRecords(context) },
                    onAddClick = { type, template -> 
                        navController.navigate(Screen.AddTransaction.createRoute(type = type, template = template)) 
                    },
                    onManageAccountsClick = { navController.navigate(Screen.AccountManagement.route) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
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
                    cardComputerRegistry = appContainer.cardComputerRegistry,
                    viewContext = viewContext
                )
            }

            composable(
                route = Screen.Settings.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onManageAccountsClick = { navController.navigate(Screen.AccountManagement.route) },
                    onManageTagsClick = { navController.navigate(Screen.ManageTags.route) },
                    onManageCategoriesClick = { navController.navigate(Screen.ManageCategories.route) },
                    onTemplatesClick = { navController.navigate(Screen.Templates.route) },
                    onManageFiltersClick = { navController.navigate(Screen.ManageFilters.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TransactionList.route,
                arguments = listOf(
                    navArgument("filterContext") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("topLevel") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                ),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val filterContext = backStackEntry.arguments?.getString("filterContext")
                val topLevel = backStackEntry.arguments?.getBoolean("topLevel") ?: false
                TransactionListScreen(
                    onMenuClick = onOpenDrawer,
                    isTopLevelParam = topLevel,
                    viewModel = transactionViewModel,
                    accountViewModel = accountViewModel,
                    analyticsViewModel = analyticsViewModel,
                    settingsViewModel = settingsViewModel,
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
                ),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
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

            composable(
                route = Screen.Analytics.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                AnalyticsScreen(
                    onMenuClick = onOpenDrawer,
                    viewModel = analyticsViewModel,
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    templateViewModel = templateViewModel,
                    settingsViewModel = settingsViewModel,
                    onRecordsClick = { context -> navController.navigateToRecords(context) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                    onEditTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onAddClick = { type, template ->
                        navController.navigate(Screen.AddTransaction.createRoute(type = type, template = template))
                    },
                    cardComputerRegistry = appContainer.cardComputerRegistry
                )
            }

            composable(
                route = Screen.SharedWallet.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                SharedWalletScreen(
                    viewModel = sharedWalletViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AccountSetup.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                AccountSetupScreen(
                    onBackClick = { navController.popBackStack() },
                    onAccountCreated = { navController.popBackStack() },
                    viewModel = accountViewModel
                )
            }

            composable(
                route = Screen.AccountManagement.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                AccountManagementScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddAccountClick = { navController.navigate(Screen.AccountSetup.route) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                    viewModel = accountViewModel
                )
            }

            composable(
                route = Screen.ExchangeRates.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                ExchangeRatesScreen(
                    viewModel = currencyViewModel,
                    onMenuClick = onOpenDrawer
                )
            }

            composable(
                route = Screen.Templates.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                TemplatesScreen(
                    viewModel = templateViewModel,
                    accountViewModel = accountViewModel,
                    onAddTemplateClick = { id -> navController.navigate(Screen.AddTemplate.createRoute(id)) },
                    onMenuClick = onOpenDrawer
                )
            }

            composable(
                route = Screen.AddTemplate.route,
                arguments = listOf(navArgument("templateId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
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
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
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
                arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
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
                    cardComputerRegistry = appContainer.cardComputerRegistry,
                    templateViewModel = templateViewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditAccountClick = { id -> navController.navigate(Screen.EditAccount.createRoute(id)) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
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
                arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
                EditAccountScreen(
                    accountId = accountId,
                    viewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ManageTags.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                ManageTagsScreen(
                    viewModel = tagViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ManageCategories.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                CategoryManagementScreen(
                    viewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ManageFilters.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
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
                }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
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

            composable(
                route = Screen.PlannedPayments.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                val plannedPaymentViewModel: com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel(
                            appContainer.plannedPaymentRepository,
                            appContainer.settingsRepository
                        )
                    }
                )
                com.osscabinet.ossWallet.ui.screens.PlannedPaymentsScreen(
                    viewModel = plannedPaymentViewModel,
                    transactionManager = appContainer.transactionManager,
                    onAddClick = { navController.navigate(Screen.PlannedPaymentEntry.createRoute()) },
                    onEditClick = { id -> navController.navigate(Screen.PlannedPaymentEntry.createRoute(id)) },
                    onDetailClick = { id -> navController.navigate(Screen.PlannedPaymentDetail.createRoute(id)) },
                    onMenuClick = onOpenDrawer
                )
            }

            composable(
                route = Screen.BudgetList.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                val budgetViewModel: com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel(
                            appContainer.budgetRepository,
                            appContainer.settingsRepository,
                            appContainer.transactionRepository,
                            appContainer.currencyRepository,
                            appContainer.categoryRepository
                        )
                    }
                )
                val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
                val precision by settingsViewModel.decimalPrecision.collectAsStateWithLifecycle()
                com.osscabinet.ossWallet.ui.screens.BudgetListScreen(
                    viewModel = budgetViewModel,
                    onAddClick = { navController.navigate(Screen.BudgetEntry.createRoute()) },
                    onEditClick = { id -> navController.navigate(Screen.BudgetEntry.createRoute(id)) },
                    onDetailClick = { id -> navController.navigate(Screen.BudgetDetail.createRoute(id)) },
                    onMenuClick = onOpenDrawer,
                    baseCurrency = baseCurrency,
                    precision = precision
                )
            }

            composable(
                route = Screen.BudgetEntry.route,
                arguments = listOf(navArgument("budgetId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: -1L
                val budgetViewModel: com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel(
                            appContainer.budgetRepository,
                            appContainer.settingsRepository,
                            appContainer.transactionRepository,
                            appContainer.currencyRepository,
                            appContainer.categoryRepository
                        )
                    }
                )
                val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
                val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
                com.osscabinet.ossWallet.ui.screens.BudgetEntryScreen(
                    budgetId = if (budgetId == -1L) null else budgetId,
                    viewModel = budgetViewModel,
                    accounts = accounts,
                    categories = categories,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PlannedPaymentEntry.route,
                arguments = listOf(navArgument("plannedPaymentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val plannedPaymentId = backStackEntry.arguments?.getLong("plannedPaymentId") ?: -1L
                val plannedPaymentViewModel: com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel(
                            appContainer.plannedPaymentRepository,
                            appContainer.settingsRepository
                        )
                    }
                )
                val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
                val allTags by tagViewModel.tags.collectAsStateWithLifecycle()
                com.osscabinet.ossWallet.ui.screens.PlannedPaymentEntryScreen(
                    plannedPaymentId = if (plannedPaymentId == -1L) null else plannedPaymentId,
                    viewModel = plannedPaymentViewModel,
                    accountViewModel = accountViewModel,
                    categories = categories,
                    availableTags = allTags,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.LoanList.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                val loanViewModel: com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel(
                            appContainer.loanRepository,
                            appContainer.settingsRepository
                        )
                    }
                )
                com.osscabinet.ossWallet.ui.screens.LoansScreen(
                    viewModel = loanViewModel,
                    onAddClick = { navController.navigate(Screen.LoanEntry.createRoute()) },
                    onLoanClick = { id -> navController.navigate(Screen.LoanDetail.createRoute(id)) },
                    onMenuClick = onOpenDrawer
                )
            }

            composable(
                route = Screen.LoanEntry.route,
                arguments = listOf(navArgument("loanId") {
                    type = NavType.LongType; defaultValue = -1L
                }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: -1L
                val loanViewModel: com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel(
                            appContainer.loanRepository,
                            appContainer.settingsRepository
                        )
                    }
                )
                val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
                com.osscabinet.ossWallet.ui.screens.LoanEntryScreen(
                    loanId = if (loanId == -1L) null else loanId,
                    viewModel = loanViewModel,
                    accounts = accounts,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.LoanDetail.route,
                arguments = listOf(navArgument("loanId") { type = NavType.LongType }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: return@composable
                val loanViewModel: com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel(
                            appContainer.loanRepository,
                            appContainer.settingsRepository,
                            appContainer.transactionRepository
                        )
                    }
                )
                val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
                val precision by settingsViewModel.decimalPrecision.collectAsStateWithLifecycle()
                com.osscabinet.ossWallet.ui.screens.LoanDetailScreen(
                    loanId = loanId,
                    viewModel = loanViewModel,
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    baseCurrency = baseCurrency,
                    precision = precision,
                    onEditClick = { navController.navigate(Screen.LoanEntry.createRoute(loanId)) },
                    onBackClick = { navController.popBackStack() },
                    onTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onRecordsClick = { context -> navController.navigateToRecords(context) }
                )
            }

            composable(
                route = Screen.PlannedPaymentDetail.route,
                arguments = listOf(navArgument("plannedPaymentId") { type = NavType.LongType }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val plannedPaymentId = backStackEntry.arguments?.getLong("plannedPaymentId") ?: return@composable
                val plannedPaymentViewModel: com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel(
                            appContainer.plannedPaymentRepository,
                            appContainer.settingsRepository
                        )
                    }
                )
                val detailAccounts by accountViewModel.accounts.collectAsStateWithLifecycle()
                val detailCategories by transactionViewModel.categories.collectAsStateWithLifecycle()
                com.osscabinet.ossWallet.ui.screens.PlannedPaymentDetailScreen(
                    plannedPaymentId = plannedPaymentId,
                    viewModel = plannedPaymentViewModel,
                    transactionManager = appContainer.transactionManager,
                    accounts = detailAccounts,
                    categories = detailCategories,
                    onEditClick = { id -> navController.navigate(Screen.PlannedPaymentEntry.createRoute(id)) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.BudgetDetail.route,
                arguments = listOf(navArgument("budgetId") { type = NavType.LongType }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: return@composable
                val budgetViewModel: com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel(
                            appContainer.budgetRepository,
                            appContainer.settingsRepository,
                            appContainer.transactionRepository,
                            appContainer.currencyRepository,
                            appContainer.categoryRepository
                        )
                    }
                )
                val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
                val precision by settingsViewModel.decimalPrecision.collectAsStateWithLifecycle()
                com.osscabinet.ossWallet.ui.screens.BudgetDetailScreen(
                    budgetId = budgetId,
                    viewModel = budgetViewModel,
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    baseCurrency = baseCurrency,
                    precision = precision,
                    onEditClick = { id -> navController.navigate(Screen.BudgetEntry.createRoute(id)) },
                    onBackClick = { navController.popBackStack() },
                    onTransactionClick = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                    onRecordsClick = { context -> navController.navigateToRecords(context) }
                )
            }
        }
    }
}
