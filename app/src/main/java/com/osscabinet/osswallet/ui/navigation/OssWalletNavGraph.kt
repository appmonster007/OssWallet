package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.di.AppContainer
import com.osscabinet.ossWallet.ui.screens.dashboard.*
import com.osscabinet.ossWallet.ui.util.GenericViewModelFactory
import com.osscabinet.ossWallet.ui.screens.accounts.*
import com.osscabinet.ossWallet.ui.screens.budgets.*
import com.osscabinet.ossWallet.ui.screens.loans.*
import com.osscabinet.ossWallet.ui.screens.plannedpayments.*
import com.osscabinet.ossWallet.ui.screens.templates.*
import com.osscabinet.ossWallet.ui.screens.transactions.*
import com.osscabinet.ossWallet.ui.screens.settings.*
import com.osscabinet.ossWallet.ui.screens.analytics.*
import com.osscabinet.ossWallet.ui.screens.tags.*
import com.osscabinet.ossWallet.ui.screens.onboarding.OnboardingScreen
import com.osscabinet.ossWallet.ui.screens.smartsuggestions.SmartSuggestionsScreen
import com.osscabinet.ossWallet.ui.screens.sharedwallet.SharedWalletViewModel
import com.osscabinet.ossWallet.ui.screens.sharedwallet.SharedWalletScreen
import com.osscabinet.ossWallet.ui.screens.accounts.entry.*
import com.osscabinet.ossWallet.ui.screens.accounts.list.*
import com.osscabinet.ossWallet.ui.screens.accounts.detail.*
import com.osscabinet.ossWallet.ui.screens.budgets.entry.*
import com.osscabinet.ossWallet.ui.screens.budgets.list.*
import com.osscabinet.ossWallet.ui.screens.budgets.detail.*
import com.osscabinet.ossWallet.ui.screens.loans.entry.*
import com.osscabinet.ossWallet.ui.screens.loans.list.*
import com.osscabinet.ossWallet.ui.screens.loans.detail.*
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.*
import com.osscabinet.ossWallet.ui.screens.plannedpayments.list.*
import com.osscabinet.ossWallet.ui.screens.plannedpayments.detail.*
import com.osscabinet.ossWallet.ui.screens.templates.entry.*
import com.osscabinet.ossWallet.ui.screens.templates.list.*
import com.osscabinet.ossWallet.ui.screens.transactions.entry.*
import com.osscabinet.ossWallet.ui.screens.transactions.list.*
import com.osscabinet.ossWallet.ui.screens.categories.list.*
import com.osscabinet.ossWallet.ui.screens.tags.list.*
import com.osscabinet.ossWallet.ui.screens.filters.entry.*
import com.osscabinet.ossWallet.ui.screens.filters.list.*
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
            appContainer.transactionManager,
            appContainer.plannedPaymentRepository,
            appContainer.templateRepository
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
                appContainer.computeCardUseCase,
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
                        defaultValue = true
                    }
                ),
                enterTransition = { FadeEnterTransitionSpec },
                exitTransition = { FadeExitTransitionSpec },
                popEnterTransition = { FadeEnterTransitionSpec },
                popExitTransition = { FadeExitTransitionSpec }
            ) { backStackEntry ->
                val viewContext = backStackEntry.arguments?.getString("viewContext")
                val topLevel = backStackEntry.arguments?.getBoolean("topLevel") ?: true
                DashboardScreen(
                    onMenuClick = onOpenDrawer,
                    onBackClick = { navController.popBackStack() },
                    isTopLevelParam = topLevel,
                    onRecordsClick = { filter -> navController.navigateToRecords(filter) },
                    onAddClick = { type, templateId ->
                        if (templateId != null) {
                            transactionViewModel.setTemplatePrefill(templateId)
                            navController.navigate(Screen.TransactionEntry.createRoute())
                        } else {
                            navController.navigate(Screen.TransactionEntry.createRoute(type = type))
                        }
                    },
                    onManageAccountsClick = { navController.navigate(Screen.AccountList.route) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onEditTransactionClick = { id -> navController.navigate(Screen.TransactionEntry.createRoute(transactionId = id)) },
                    onCloneTransactionClick = { id ->
                        transactionViewModel.setCloneTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onSplitTransactionClick = { id ->
                        transactionViewModel.setSplitTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
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
                    onManageAccountsClick = { navController.navigate(Screen.AccountList.route) },
                    onManageTagsClick = { navController.navigate(Screen.TagList.route) },
                    onManageCategoriesClick = { navController.navigate(Screen.CategoryList.route) },
                    onTemplatesClick = { navController.navigate(Screen.TemplateList.route) },
                    onManageFiltersClick = { navController.navigate(Screen.FilterList.route) },
                    onSmartSuggestionsClick = { navController.navigate(Screen.SmartSuggestions.route) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.SmartSuggestions.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                SmartSuggestionsScreen(
                    viewModel = transactionViewModel,
                    onAcceptAsTemplate = { navController.navigate(Screen.TemplateEntry.createRoute()) },
                    onAcceptAsPlannedPayment = { navController.navigate(Screen.PlannedPaymentEntry.createRoute()) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TransactionList.route,
                arguments = listOf(
                    navArgument("filter") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("topLevel") {
                        type = NavType.BoolType
                        defaultValue = true
                    }
                ),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val filterJson = backStackEntry.arguments?.getString("filter")
                val topLevel = backStackEntry.arguments?.getBoolean("topLevel") ?: true
                TransactionListScreen(
                    onMenuClick = onOpenDrawer,
                    isTopLevelParam = topLevel,
                    viewModel = transactionViewModel,
                    accountViewModel = accountViewModel,
                    analyticsViewModel = analyticsViewModel,
                    settingsViewModel = settingsViewModel,
                    onEditTransactionClick = { id -> navController.navigate(Screen.TransactionEntry.createRoute(transactionId = id)) },
                    onCloneTransactionClick = { id ->
                        transactionViewModel.setCloneTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onSplitTransactionClick = { id ->
                        transactionViewModel.setSplitTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onBackClick = { navController.popBackStack() },
                    templateViewModel = templateViewModel,
                    filterJson = filterJson
                )
            }

            composable(
                route = Screen.TransactionEntry.route,
                arguments = listOf(
                    navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("accountId") { type = NavType.StringType; nullable = true },
                    navArgument("type") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("templateId") { type = NavType.LongType; defaultValue = -1L }
                ),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: -1L
                val accountId = backStackEntry.arguments?.getString("accountId")
                val type = backStackEntry.arguments?.getInt("type") ?: -1
                val templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L
                val prefillDto by transactionViewModel.prefillDto.collectAsStateWithLifecycle()
                
                val entryViewModel: TransactionEntryViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        TransactionEntryViewModel(
                            savedStateHandle = androidx.lifecycle.SavedStateHandle(),
                            transactionUseCases = appContainer.transactionUseCases,
                            transactionManager = appContainer.transactionManager,
                            settingsRepository = appContainer.settingsRepository,
                            computationRepository = appContainer.computationRepository,
                            accountRepository = appContainer.accountRepository,
                            categoryRepository = appContainer.categoryRepository,
                            tagRepository = appContainer.tagRepository,
                            templateRepository = appContainer.templateRepository,
                            plannedPaymentRepository = appContainer.plannedPaymentRepository
                        )
                    }
                )

                TransactionEntryScreen(
                    transactionId = if (transactionId == -1L) null else transactionId,
                    accountId = accountId,
                    initialType = if (type == -1) null else type,
                    initialTemplateId = if (templateId == -1L) null else templateId,
                    prefillDto = prefillDto,
                    viewModel = entryViewModel,
                    onBackClick = { navController.popBackStack() },
                    onDispose = { transactionViewModel.clearPrefillDto() }
                )
            }

            composable(
                route = Screen.Analytics.route,
                arguments = listOf(
                    navArgument("topLevel") {
                        type = NavType.BoolType
                        defaultValue = true
                    }
                ),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val topLevel = backStackEntry.arguments?.getBoolean("topLevel") ?: true
                AnalyticsScreen(
                    onMenuClick = onOpenDrawer,
                    onBackClick = { navController.popBackStack() },
                    isTopLevel = topLevel,
                    viewModel = analyticsViewModel,
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    templateViewModel = templateViewModel,
                    settingsViewModel = settingsViewModel,
                    onRecordsClick = { filter -> navController.navigateToRecords(filter) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                    onEditTransactionClick = { id -> navController.navigate(Screen.TransactionEntry.createRoute(transactionId = id)) },
                    onCloneTransactionClick = { id ->
                        transactionViewModel.setCloneTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onSplitTransactionClick = { id ->
                        transactionViewModel.setSplitTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onAddClick = { type, templateId ->
                        if (templateId != null) {
                            transactionViewModel.setTemplatePrefill(templateId)
                            navController.navigate(Screen.TransactionEntry.createRoute())
                        } else {
                            navController.navigate(Screen.TransactionEntry.createRoute(type = type))
                        }
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
                route = Screen.AccountEntry.route,
                arguments = listOf(navArgument("accountId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId")
                val entryViewModel: AccountEntryViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        AccountEntryViewModel(
                            accountRepository = appContainer.accountRepository,
                            transactionRepository = appContainer.transactionRepository,
                            computationRepository = appContainer.computationRepository,
                            settingsRepository = appContainer.settingsRepository
                        )
                    }
                )
                AccountEntryScreen(
                    accountId = accountId,
                    viewModel = entryViewModel,
                    onBackClick = { navController.popBackStack() },
                    onDone = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AccountList.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                AccountListScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddAccountClick = { navController.navigate(Screen.AccountEntry.createRoute()) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                    viewModel = accountViewModel
                )
            }

            composable(
                route = Screen.ExchangeRates.route,
                arguments = listOf(
                    navArgument("topLevel") {
                        type = NavType.BoolType
                        defaultValue = true
                    }
                ),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val topLevel = backStackEntry.arguments?.getBoolean("topLevel") ?: true
                ExchangeRatesScreen(
                    onMenuClick = onOpenDrawer,
                    onBackClick = { navController.popBackStack() },
                    isTopLevel = topLevel,
                    viewModel = currencyViewModel
                )
            }

            composable(
                route = Screen.TemplateList.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
                val categoryMap = remember(categories) { categories.associateBy { it.id } }
                
                TemplateListScreen(
                    viewModel = templateViewModel,
                    accountViewModel = accountViewModel,
                    categoryMap = categoryMap,
                    onAddTemplateClick = { id -> navController.navigate(Screen.TemplateEntry.createRoute(id)) },
                    onCreatePlannedPayment = { template ->
                        transactionViewModel.setPlannedPaymentPrefillFromTemplate(template)
                        navController.navigate(Screen.PlannedPaymentEntry.route)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TemplateEntry.route,
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
                val prefill by transactionViewModel.templatePrefill.collectAsStateWithLifecycle()
                
                val entryViewModel: TemplateEntryViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        TemplateEntryViewModel(
                            templateRepository = appContainer.templateRepository,
                            accountRepository = appContainer.accountRepository,
                            categoryRepository = appContainer.categoryRepository,
                            tagRepository = appContainer.tagRepository,
                            transactionUseCases = appContainer.transactionUseCases,
                            settingsRepository = appContainer.settingsRepository
                        )
                    }
                )

                LaunchedEffect(prefill) {
                    if (prefill != null) {
                        transactionViewModel.clearTemplatePrefill()
                    }
                }

                TemplateEntryScreen(
                    templateId = if (templateId == -1L) null else templateId,
                    prefill = prefill,
                    viewModel = entryViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }


            composable(
                route = Screen.AccountDetail.route,
                arguments = listOf(
                    navArgument("accountId") { type = NavType.StringType },
                    navArgument("filter") { type = NavType.StringType; nullable = true; defaultValue = null }
                ),
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
                val filterJson = backStackEntry.arguments?.getString("filter")
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
                    filterJson = filterJson,
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    settingsViewModel = settingsViewModel,
                    analyticsViewModel = analyticsViewModel,
                    accountDetailViewModel = accountDetailViewModel,
                    cardComputerRegistry = appContainer.cardComputerRegistry,
                    templateViewModel = templateViewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditAccountClick = { id -> navController.navigate(Screen.AccountEntry.createRoute(id)) },
                    onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                    onEditTransactionClick = { id -> navController.navigate(Screen.TransactionEntry.createRoute(transactionId = id)) },
                    onCloneTransactionClick = { id ->
                        transactionViewModel.setCloneTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onSplitTransactionClick = { id ->
                        transactionViewModel.setSplitTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onAddTransactionClick = { accId, type, templateId ->
                        navController.navigate(Screen.TransactionEntry.createRoute(accountId = accId, type = type, templateId = templateId))
                    },
                    onRecordsClick = { filter -> navController.navigateToRecords(filter) }
                )
            }


            composable(
                route = Screen.TagList.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                TagListScreen(
                    viewModel = tagViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CategoryList.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                CategoryListScreen(
                    viewModel = transactionViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.FilterList.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                FilterListScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { navController.popBackStack() },
                    onAddFilterClick = { 
                        navController.navigate(Screen.FilterEntry.createRoute())
                    },
                    onEditFilterClick = { filter -> 
                        navController.navigate(Screen.FilterEntry.createRoute(filter.id))
                    }
                )
            }

            composable(
                route = Screen.FilterEntry.route,
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
                
                FilterEntryScreen(
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
                route = Screen.PlannedPaymentList.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                val plannedPaymentViewModel: PlannedPaymentViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        PlannedPaymentViewModel(
                            appContainer.plannedPaymentRepository,
                            appContainer.templateRepository,
                            appContainer.settingsRepository
                        )
                    }
                )
                PlannedPaymentListScreen(
                    viewModel = plannedPaymentViewModel,
                    transactionManager = appContainer.transactionManager,
                    onAddClick = { navController.navigate(Screen.PlannedPaymentEntry.createRoute()) },
                    onEditClick = { id -> navController.navigate(Screen.PlannedPaymentEntry.createRoute(id)) },
                    onDetailClick = { id -> navController.navigate(Screen.PlannedPaymentDetail.createRoute(id)) },
                    onConfirmClick = { id, date ->
                        transactionViewModel.setPlannedPaymentPrefill(id, date)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.BudgetList.route,
                enterTransition = { EnterTransitionSpec },
                exitTransition = { ExitTransitionSpec },
                popEnterTransition = { PopEnterTransitionSpec },
                popExitTransition = { PopExitTransitionSpec }
            ) {
                val budgetViewModel: BudgetViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        BudgetViewModel(
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
                BudgetListScreen(
                    viewModel = budgetViewModel,
                    onAddClick = { navController.navigate(Screen.BudgetEntry.createRoute()) },
                    onEditClick = { id -> navController.navigate(Screen.BudgetEntry.createRoute(id)) },
                    onDetailClick = { id -> navController.navigate(Screen.BudgetDetail.createRoute(id)) },
                    onBackClick = { navController.popBackStack() },
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
                val entryViewModel: BudgetEntryViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        BudgetEntryViewModel(
                            budgetRepository = appContainer.budgetRepository,
                            accountRepository = appContainer.accountRepository,
                            categoryRepository = appContainer.categoryRepository,
                            tagRepository = appContainer.tagRepository,
                            transactionRepository = appContainer.transactionRepository,
                            settingsRepository = appContainer.settingsRepository
                        )
                    }
                )
                BudgetEntryScreen(
                    budgetId = if (budgetId == -1L) null else budgetId,
                    viewModel = entryViewModel,
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
                val entryViewModel: PlannedPaymentEntryViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        PlannedPaymentEntryViewModel(
                            plannedPaymentRepository = appContainer.plannedPaymentRepository,
                            templateRepository = appContainer.templateRepository,
                            accountRepository = appContainer.accountRepository,
                            categoryRepository = appContainer.categoryRepository,
                            tagRepository = appContainer.tagRepository,
                            transactionRepository = appContainer.transactionRepository,
                            settingsRepository = appContainer.settingsRepository
                        )
                    }
                )
                PlannedPaymentEntryScreen(
                    plannedPaymentId = if (plannedPaymentId == -1L) null else plannedPaymentId,
                    viewModel = entryViewModel,
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
                val loanViewModel: LoanViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        LoanViewModel(
                            appContainer.loanRepository,
                            appContainer.settingsRepository
                        )
                    }
                )
                LoanListScreen(
                    viewModel = loanViewModel,
                    onAddClick = { navController.navigate(Screen.LoanEntry.createRoute()) },
                    onLoanClick = { id -> navController.navigate(Screen.LoanDetail.createRoute(id)) },
                    onBackClick = { navController.popBackStack() }
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
                val entryViewModel: LoanEntryViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        LoanEntryViewModel(
                            loanRepository = appContainer.loanRepository,
                            accountRepository = appContainer.accountRepository,
                            categoryRepository = appContainer.categoryRepository,
                            tagRepository = appContainer.tagRepository,
                            transactionRepository = appContainer.transactionRepository,
                            settingsRepository = appContainer.settingsRepository
                        )
                    }
                )
                LoanEntryScreen(
                    loanId = if (loanId == -1L) null else loanId,
                    viewModel = entryViewModel,
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
                val loanViewModel: LoanViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        LoanViewModel(
                            appContainer.loanRepository,
                            appContainer.settingsRepository,
                            appContainer.transactionRepository
                        )
                    }
                )
                val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
                val precision by settingsViewModel.decimalPrecision.collectAsStateWithLifecycle()
                LoanDetailScreen(
                    loanId = loanId,
                    viewModel = loanViewModel,
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    baseCurrency = baseCurrency,
                    precision = precision,
                    onEditClick = { navController.navigate(Screen.LoanEntry.createRoute(loanId)) },
                    onBackClick = { navController.popBackStack() },
                    onTransactionClick = { id -> navController.navigate(Screen.TransactionEntry.createRoute(transactionId = id)) },
                    onCloneTransactionClick = { id ->
                        transactionViewModel.setCloneTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onSplitTransactionClick = { id ->
                        transactionViewModel.setSplitTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onRecordsClick = { filter -> navController.navigateToRecords(filter) }
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
                val plannedPaymentViewModel: PlannedPaymentViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        PlannedPaymentViewModel(
                            appContainer.plannedPaymentRepository,
                            appContainer.templateRepository,
                            appContainer.settingsRepository
                        )
                    }
                )
                val detailAccounts by accountViewModel.accounts.collectAsStateWithLifecycle()
                val detailCategories by transactionViewModel.categories.collectAsStateWithLifecycle()
                PlannedPaymentDetailScreen(
                    plannedPaymentId = plannedPaymentId,
                    viewModel = plannedPaymentViewModel,
                    transactionManager = appContainer.transactionManager,
                    accounts = detailAccounts,
                    categories = detailCategories,
                    onEditClick = { id -> navController.navigate(Screen.PlannedPaymentEntry.createRoute(id)) },
                    onConfirmClick = { id, date ->
                        transactionViewModel.setPlannedPaymentPrefill(id, date)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
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
                val budgetViewModel: BudgetViewModel = viewModel(
                    factory = GenericViewModelFactory {
                        BudgetViewModel(
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
                BudgetDetailScreen(
                    budgetId = budgetId,
                    viewModel = budgetViewModel,
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel,
                    baseCurrency = baseCurrency,
                    precision = precision,
                    onEditClick = { id -> navController.navigate(Screen.BudgetEntry.createRoute(id)) },
                    onBackClick = { navController.popBackStack() },
                    onTransactionClick = { id -> navController.navigate(Screen.TransactionEntry.createRoute(transactionId = id)) },
                    onCloneTransactionClick = { id ->
                        transactionViewModel.setCloneTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onSplitTransactionClick = { id ->
                        transactionViewModel.setSplitTransactionId(id)
                        navController.navigate(Screen.TransactionEntry.createRoute())
                    },
                    onRecordsClick = { filter -> navController.navigateToRecords(filter) }
                )
            }
        }
    }
}
