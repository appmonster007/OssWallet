package com.osscabinet.ossWallet.ui.navigation.graphs

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.osscabinet.ossWallet.ui.navigation.AccountListRoute
import com.osscabinet.ossWallet.ui.navigation.CategoryListRoute
import com.osscabinet.ossWallet.ui.navigation.CsvImportRoute
import com.osscabinet.ossWallet.ui.navigation.DashboardRoute
import com.osscabinet.ossWallet.ui.navigation.EnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.ExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.FilterListRoute
import com.osscabinet.ossWallet.ui.navigation.OpenInWebRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentEntryRoute
import com.osscabinet.ossWallet.ui.navigation.PopEnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.PopExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.SettingsCurrencyRoute
import com.osscabinet.ossWallet.ui.navigation.SettingsDataManagementRoute
import com.osscabinet.ossWallet.ui.navigation.SettingsDisplayRoute
import com.osscabinet.ossWallet.ui.navigation.SettingsRoute
import com.osscabinet.ossWallet.ui.navigation.SettingsSecurityRoute
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.navigation.ShortlistedCurrenciesRoute
import com.osscabinet.ossWallet.ui.navigation.SmartSuggestionsRoute
import com.osscabinet.ossWallet.ui.navigation.TagListRoute
import com.osscabinet.ossWallet.ui.navigation.TemplateEntryRoute
import com.osscabinet.ossWallet.ui.navigation.TemplateListRoute
import com.osscabinet.ossWallet.ui.screens.import_csv.CsvImportScreen
import com.osscabinet.ossWallet.ui.screens.import_csv.CsvImportViewModel
import com.osscabinet.ossWallet.ui.screens.openinweb.OpenInWebActions
import com.osscabinet.ossWallet.ui.screens.openinweb.OpenInWebScreen
import com.osscabinet.ossWallet.ui.screens.openinweb.OpenInWebViewModel
import com.osscabinet.ossWallet.ui.screens.settings.CurrencySettingsActions
import com.osscabinet.ossWallet.ui.screens.settings.CurrencySettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.DataManagementActions
import com.osscabinet.ossWallet.ui.screens.settings.DataManagementScreen
import com.osscabinet.ossWallet.ui.screens.settings.DisplaySettingsActions
import com.osscabinet.ossWallet.ui.screens.settings.DisplaySettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.SecuritySettingsActions
import com.osscabinet.ossWallet.ui.screens.settings.SecuritySettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.SettingsActions
import com.osscabinet.ossWallet.ui.screens.settings.SettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.ShortlistedCurrenciesActions
import com.osscabinet.ossWallet.ui.screens.settings.ShortlistedCurrenciesScreen
import com.osscabinet.ossWallet.ui.screens.smartsuggestions.SmartSuggestionsActions
import com.osscabinet.ossWallet.ui.screens.smartsuggestions.SmartSuggestionsScreen

fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    vms: SharedNavViewModels,
) {
    composable<SettingsRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        SettingsScreen(
            viewModel = vms.settingsViewModel,
            actions = SettingsActions(
                onDisplayClick = { navController.navigate(SettingsDisplayRoute) },
                onCurrencyClick = { navController.navigate(SettingsCurrencyRoute) },
                onAccountsClick = { navController.navigate(AccountListRoute) },
                onCategoriesClick = { navController.navigate(CategoryListRoute) },
                onTagsClick = { navController.navigate(TagListRoute) },
                onTemplatesClick = { navController.navigate(TemplateListRoute) },
                onSmartSuggestionsClick = { navController.navigate(SmartSuggestionsRoute) },
                onFiltersClick = { navController.navigate(FilterListRoute) },
                onSecurityClick = { navController.navigate(SettingsSecurityRoute) },
                onDataManagementClick = { navController.navigate(SettingsDataManagementRoute) },
                onBackClick = { navController.popBackStack() }
            )
        )
    }

    composable<SettingsDisplayRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        DisplaySettingsScreen(
            viewModel = vms.settingsViewModel,
            actions = DisplaySettingsActions(
                onBackClick = { navController.popBackStack() }
            )
        )
    }

    composable<SettingsCurrencyRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        CurrencySettingsScreen(
            viewModel = vms.settingsViewModel,
            actions = CurrencySettingsActions(
                onShortlistedCurrenciesClick = { navController.navigate(ShortlistedCurrenciesRoute) },
                onBackClick = { navController.popBackStack() }
            )
        )
    }

    composable<SettingsSecurityRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        SecuritySettingsScreen(
            viewModel = vms.settingsViewModel,
            actions = SecuritySettingsActions(
                onOpenInWebClick = { navController.navigate(OpenInWebRoute) },
                onBackClick = { navController.popBackStack() }
            )
        )
    }

    composable<SettingsDataManagementRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        DataManagementScreen(
            viewModel = vms.settingsViewModel,
            actions = DataManagementActions(
                onBackClick = { navController.popBackStack() },
                onNavigateToCsvImport = { navController.navigate(CsvImportRoute) }
            )
        )
    }

    composable<ShortlistedCurrenciesRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        ShortlistedCurrenciesScreen(
            viewModel = vms.settingsViewModel,
            actions = ShortlistedCurrenciesActions(
                onBack = { navController.popBackStack() }
            )
        )
    }

    composable<SmartSuggestionsRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        SmartSuggestionsScreen(
            viewModel = vms.transactionViewModel,
            actions = SmartSuggestionsActions(
                onAcceptAsTemplate = { navController.navigate(TemplateEntryRoute()) },
                onAcceptAsPlannedPayment = { navController.navigate(PlannedPaymentEntryRoute()) },
                onBackClick = { navController.popBackStack() }
            )
        )
    }

    composable<CsvImportRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        val vm: CsvImportViewModel = hiltViewModel()
        CsvImportScreen(
            viewModel = vm,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDashboard = {
                navController.navigate(DashboardRoute()) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    composable<OpenInWebRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        val vm: OpenInWebViewModel = hiltViewModel()
        OpenInWebScreen(
            viewModel = vm,
            actions = OpenInWebActions(
                onNavigateBack = { navController.popBackStack() }
            )
        )
    }
}
