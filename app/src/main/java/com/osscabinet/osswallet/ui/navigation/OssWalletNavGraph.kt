package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.accounts.detail.AccountDetailScreen
import com.osscabinet.ossWallet.ui.screens.accounts.detail.AccountDetailViewModel
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryScreen
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryViewModel
import com.osscabinet.ossWallet.ui.screens.accounts.list.AccountListScreen
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.BudgetViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.detail.BudgetDetailScreen
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryScreen
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.list.BudgetListScreen
import com.osscabinet.ossWallet.ui.screens.categories.CategoryViewModel
import com.osscabinet.ossWallet.ui.screens.categories.list.CategoryListScreen
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardUiState
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardViewModel
import com.osscabinet.ossWallet.ui.screens.filters.FilterViewModel
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryScreen
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryViewModel
import com.osscabinet.ossWallet.ui.screens.filters.list.FilterListScreen
import com.osscabinet.ossWallet.ui.screens.loans.LoanViewModel
import com.osscabinet.ossWallet.ui.screens.loans.detail.LoanDetailScreen
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryScreen
import com.osscabinet.ossWallet.ui.screens.loans.entry.LoanEntryViewModel
import com.osscabinet.ossWallet.ui.screens.loans.list.LoanListScreen
import com.osscabinet.ossWallet.ui.screens.main.MainDrawerShell
import com.osscabinet.ossWallet.ui.screens.main.MainViewModel
import com.osscabinet.ossWallet.ui.screens.onboarding.OnboardingScreen
import com.osscabinet.ossWallet.ui.screens.onboarding.OnboardingViewModel
import com.osscabinet.ossWallet.ui.screens.openinweb.OpenInWebScreen
import com.osscabinet.ossWallet.ui.screens.openinweb.OpenInWebViewModel
import com.osscabinet.ossWallet.ui.screens.plannedpayments.PlannedPaymentViewModel
import com.osscabinet.ossWallet.ui.screens.plannedpayments.detail.PlannedPaymentDetailScreen
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryScreen
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryViewModel
import com.osscabinet.ossWallet.ui.screens.plannedpayments.list.PlannedPaymentListScreen
import com.osscabinet.ossWallet.ui.screens.settings.CurrencySettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.CurrencyViewModel
import com.osscabinet.ossWallet.ui.screens.settings.DataSettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.DisplaySettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.SecuritySettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.SettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.settings.ShortlistedCurrenciesScreen
import com.osscabinet.ossWallet.ui.screens.sharedwallet.SharedWalletScreen
import com.osscabinet.ossWallet.ui.screens.sharedwallet.SharedWalletViewModel
import com.osscabinet.ossWallet.ui.screens.smartsuggestions.SmartSuggestionsScreen
import com.osscabinet.ossWallet.ui.screens.tags.TagViewModel
import com.osscabinet.ossWallet.ui.screens.tags.list.TagListScreen
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryScreen
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryViewModel
import com.osscabinet.ossWallet.ui.screens.templates.list.TemplateListScreen
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryScreen
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryViewModel
import kotlinx.serialization.json.Json


@Composable
fun OssWalletNavGraph(
    navController: NavHostController,
    onSplashConditionChanged: (Boolean) -> Unit = {}
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()
    val categoryViewModel: CategoryViewModel = hiltViewModel()
    val transactionViewModel: TransactionViewModel = hiltViewModel()
    val accountViewModel: AccountViewModel = hiltViewModel()
    val sharedWalletViewModel: SharedWalletViewModel = hiltViewModel()
    val templateViewModel: TemplateViewModel = hiltViewModel()
    val currencyViewModel: CurrencyViewModel = hiltViewModel()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val tagViewModel: TagViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()

    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val isOnboardingCompleted by settingsViewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

    val keepSplashOnScreen = dashboardUiState is DashboardUiState.Loading

    SideEffect {
        onSplashConditionChanged(keepSplashOnScreen)
    }

    NavHost(
        navController = navController,
        startDestination = if (isOnboardingCompleted == true) Screen.Main.createRoute() else Screen.Onboarding.route
    ) {
        composable(
            route = Screen.Onboarding.route,
            enterTransition = { FadeEnterTransitionSpec },
            exitTransition = { FadeExitTransitionSpec },
            popEnterTransition = { FadeEnterTransitionSpec },
            popExitTransition = { FadeExitTransitionSpec }
        ) {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onComplete = {
                    navController.navigate(Screen.Main.createRoute()) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Main.route,
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                }
            ),
            enterTransition = { FadeEnterTransitionSpec },
            exitTransition = { FadeExitTransitionSpec },
            popEnterTransition = { FadeEnterTransitionSpec },
            popExitTransition = { FadeExitTransitionSpec }
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab")
            MainDrawerShell(
                rootNavController = navController,
                mainViewModel = mainViewModel,
                initialTab = tab,
                dashboardViewModel = dashboardViewModel,
                transactionViewModel = transactionViewModel,
                accountViewModel = accountViewModel,
                analyticsViewModel = analyticsViewModel,
                currencyViewModel = currencyViewModel,
                templateViewModel = templateViewModel,
                settingsViewModel = settingsViewModel
            )
        }

        // Removed Dashboard from root host - now in MainDrawerShell

        composable(
            route = Screen.Settings.route,
            enterTransition = { EnterTransitionSpec },
            exitTransition = { ExitTransitionSpec },
            popEnterTransition = { PopEnterTransitionSpec },
            popExitTransition = { PopExitTransitionSpec }
        ) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onDisplayClick = { navController.navigate(Screen.SettingsDisplay.route) },
                onCurrencyClick = { navController.navigate(Screen.SettingsCurrency.route) },
                onDataClick = { navController.navigate(Screen.SettingsData.route) },
                onSecurityClick = { navController.navigate(Screen.SettingsSecurity.route) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SettingsDisplay.route,
            enterTransition = { EnterTransitionSpec },
            exitTransition = { ExitTransitionSpec },
            popEnterTransition = { PopEnterTransitionSpec },
            popExitTransition = { PopExitTransitionSpec }
        ) {
            DisplaySettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SettingsCurrency.route,
            enterTransition = { EnterTransitionSpec },
            exitTransition = { ExitTransitionSpec },
            popEnterTransition = { PopEnterTransitionSpec },
            popExitTransition = { PopExitTransitionSpec }
        ) {
            CurrencySettingsScreen(
                viewModel = settingsViewModel,
                onShortlistedCurrenciesClick = { navController.navigate(Screen.ShortlistedCurrencies.route) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SettingsData.route,
            enterTransition = { EnterTransitionSpec },
            exitTransition = { ExitTransitionSpec },
            popEnterTransition = { PopEnterTransitionSpec },
            popExitTransition = { PopExitTransitionSpec }
        ) {
            DataSettingsScreen(
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
            route = Screen.SettingsSecurity.route,
            enterTransition = { EnterTransitionSpec },
            exitTransition = { ExitTransitionSpec },
            popEnterTransition = { PopEnterTransitionSpec },
            popExitTransition = { PopExitTransitionSpec }
        ) {
            SecuritySettingsScreen(
                viewModel = settingsViewModel,
                onOpenInWebClick = { navController.navigate(Screen.OpenInWeb.route) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ShortlistedCurrencies.route,
            enterTransition = { EnterTransitionSpec },
            exitTransition = { ExitTransitionSpec },
            popEnterTransition = { PopEnterTransitionSpec },
            popExitTransition = { PopExitTransitionSpec }
        ) {
            ShortlistedCurrenciesScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
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

            val entryViewModel: TransactionEntryViewModel = hiltViewModel()

            TransactionEntryScreen(
                transactionId = if (transactionId == -1L) null else transactionId,
                accountId = accountId,
                initialType = if (type == -1) null else TransactionType.fromInt(type),
                initialTemplateId = if (templateId == -1L) null else templateId,
                prefillDto = prefillDto,
                viewModel = entryViewModel,
                onBackClick = { navController.popBackStack() },
                onDispose = { transactionViewModel.clearPrefillDto() }
            )
        }

        // Analytics is now in MainDrawerShell

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
            val entryViewModel: AccountEntryViewModel = hiltViewModel()
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

        // ExchangeRates is now in MainDrawerShell

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
                onAddTemplateClick = { id ->
                    navController.navigate(
                        Screen.TemplateEntry.createRoute(
                            id
                        )
                    )
                },
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

            val entryViewModel: TemplateEntryViewModel = hiltViewModel()

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
                navArgument("filter") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                }
            ),
            enterTransition = { EnterTransitionSpec },
            exitTransition = { ExitTransitionSpec },
            popEnterTransition = { PopEnterTransitionSpec },
            popExitTransition = { PopExitTransitionSpec }
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            val filterJson = backStackEntry.arguments?.getString("filter")
            val accountDetailViewModel: AccountDetailViewModel = hiltViewModel()
            AccountDetailScreen(
                accountId = accountId,
                filterJson = filterJson,
                accountViewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                settingsViewModel = settingsViewModel,
                analyticsViewModel = analyticsViewModel,
                accountDetailViewModel = accountDetailViewModel,
                cardComputerRegistry = dashboardViewModel.cardComputerRegistry,
                templateViewModel = templateViewModel,
                onBackClick = { navController.popBackStack() },
                onEditAccountClick = { id ->
                    navController.navigate(
                        Screen.AccountEntry.createRoute(
                            id
                        )
                    )
                },
                onAccountClick = { id -> navController.navigate(Screen.AccountDetail.createRoute(id)) },
                onNavigateToPlannedPaymentList = { navController.navigate(Screen.PlannedPaymentList.route) },
                onNavigateToBudgetList = { navController.navigate(Screen.BudgetList.route) },
                onNavigateToLoanList = { navController.navigate(Screen.LoanList.route) },
                onNavigateToPlannedPaymentDetail = { id ->
                    navController.navigate(
                        Screen.PlannedPaymentDetail.createRoute(
                            id
                        )
                    )
                },
                onNavigateToBudgetDetail = { id ->
                    navController.navigate(
                        Screen.BudgetDetail.createRoute(
                            id
                        )
                    )
                },
                onNavigateToLoanDetail = { id ->
                    navController.navigate(
                        Screen.LoanDetail.createRoute(
                            id
                        )
                    )
                },
                onEditTransactionClick = { id ->
                    navController.navigate(
                        Screen.TransactionEntry.createRoute(
                            transactionId = id
                        )
                    )
                },
                onCloneTransactionClick = { id ->
                    transactionViewModel.setCloneTransactionId(id)
                    navController.navigate(Screen.TransactionEntry.createRoute())
                },
                onSplitTransactionClick = { id ->
                    transactionViewModel.setSplitTransactionId(id)
                    navController.navigate(Screen.TransactionEntry.createRoute())
                },
                onAddTransactionClick = { accId, type, templateId ->
                    navController.navigate(
                        Screen.TransactionEntry.createRoute(
                            accountId = accId,
                            type = type,
                            templateId = templateId
                        )
                    )
                },
                onRecordsClick = { filter -> navController.navigateToRecords(filter) },
                onSaveFilterClick = { filter ->
                    val filterJson =
                        java.net.URLEncoder.encode(Json.encodeToString(filter), "UTF-8")
                    navController.navigate(Screen.FilterEntry.createRoute(initialFilter = filterJson))
                }
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
                viewModel = categoryViewModel,
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
            val filterViewModel: FilterViewModel = hiltViewModel()
            FilterListScreen(
                viewModel = filterViewModel,
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
            arguments = listOf(
                navArgument("filterId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("initialFilter") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { EnterTransitionSpec },
            exitTransition = { ExitTransitionSpec },
            popEnterTransition = { PopEnterTransitionSpec },
            popExitTransition = { PopExitTransitionSpec }
        ) { backStackEntry ->
            val filterId = backStackEntry.arguments?.getLong("filterId") ?: -1L
            val initialFilterJson = backStackEntry.arguments?.getString("initialFilter")
            val entryViewModel: FilterEntryViewModel = hiltViewModel()

            FilterEntryScreen(
                filterId = filterId,
                initialFilterJson = initialFilterJson,
                viewModel = entryViewModel,
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
            val plannedPaymentViewModel: PlannedPaymentViewModel = hiltViewModel()
            PlannedPaymentListScreen(
                viewModel = plannedPaymentViewModel,
                transactionManager = dashboardViewModel.transactionManager,
                onAddClick = { navController.navigate(Screen.PlannedPaymentEntry.createRoute()) },
                onEditClick = { id ->
                    navController.navigate(
                        Screen.PlannedPaymentEntry.createRoute(
                            id
                        )
                    )
                },
                onDetailClick = { id ->
                    navController.navigate(
                        Screen.PlannedPaymentDetail.createRoute(
                            id
                        )
                    )
                },
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
            val budgetViewModel: BudgetViewModel = hiltViewModel()
            val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
            BudgetListScreen(
                viewModel = budgetViewModel,
                onAddClick = { navController.navigate(Screen.BudgetEntry.createRoute()) },
                onEditClick = { id -> navController.navigate(Screen.BudgetEntry.createRoute(id)) },
                onDetailClick = { id -> navController.navigate(Screen.BudgetDetail.createRoute(id)) },
                onBackClick = { navController.popBackStack() },
                baseCurrency = baseCurrency
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
            val entryViewModel: BudgetEntryViewModel = hiltViewModel()
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
            val entryViewModel: PlannedPaymentEntryViewModel = hiltViewModel()
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
            val loanViewModel: LoanViewModel = hiltViewModel()
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
            val entryViewModel: LoanEntryViewModel = hiltViewModel()
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
            val loanViewModel: LoanViewModel = hiltViewModel()
            val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
            LoanDetailScreen(
                loanId = loanId,
                viewModel = loanViewModel,
                accountViewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                baseCurrency = baseCurrency,
                onEditClick = { navController.navigate(Screen.LoanEntry.createRoute(loanId)) },
                onBackClick = { navController.popBackStack() },
                onTransactionClick = { id ->
                    navController.navigate(
                        Screen.TransactionEntry.createRoute(
                            transactionId = id
                        )
                    )
                },
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
            val plannedPaymentId =
                backStackEntry.arguments?.getLong("plannedPaymentId") ?: return@composable
            val plannedPaymentViewModel: PlannedPaymentViewModel = hiltViewModel()
            val detailAccounts by accountViewModel.accounts.collectAsStateWithLifecycle()
            val detailCategories by transactionViewModel.categories.collectAsStateWithLifecycle()
            PlannedPaymentDetailScreen(
                plannedPaymentId = plannedPaymentId,
                viewModel = plannedPaymentViewModel,
                transactionManager = dashboardViewModel.transactionManager,
                accounts = detailAccounts,
                categories = detailCategories,
                onEditClick = { id ->
                    navController.navigate(
                        Screen.PlannedPaymentEntry.createRoute(
                            id
                        )
                    )
                },
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
            val budgetViewModel: BudgetViewModel = hiltViewModel()
            val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
            BudgetDetailScreen(
                budgetId = budgetId,
                viewModel = budgetViewModel,
                accountViewModel = accountViewModel,
                transactionViewModel = transactionViewModel,
                baseCurrency = baseCurrency,
                onEditClick = { id -> navController.navigate(Screen.BudgetEntry.createRoute(id)) },
                onBackClick = { navController.popBackStack() },
                onTransactionClick = { id ->
                    navController.navigate(
                        Screen.TransactionEntry.createRoute(
                            transactionId = id
                        )
                    )
                },
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
            route = Screen.OpenInWeb.route,
            enterTransition = { EnterTransitionSpec },
            exitTransition = { ExitTransitionSpec },
            popEnterTransition = { PopEnterTransitionSpec },
            popExitTransition = { PopExitTransitionSpec }
        ) {
            val vm: OpenInWebViewModel = hiltViewModel()
            OpenInWebScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
