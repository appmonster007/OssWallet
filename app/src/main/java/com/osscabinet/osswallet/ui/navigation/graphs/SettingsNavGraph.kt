package com.osscabinet.ossWallet.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.osscabinet.ossWallet.ui.navigation.EnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.ExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.OpenInWebRoute
import com.osscabinet.ossWallet.ui.navigation.PopEnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.PopExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.SettingsCurrencyRoute
import com.osscabinet.ossWallet.ui.navigation.SettingsDataRoute
import com.osscabinet.ossWallet.ui.navigation.SettingsDisplayRoute
import com.osscabinet.ossWallet.ui.navigation.SettingsRoute
import com.osscabinet.ossWallet.ui.navigation.SettingsSecurityRoute
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.navigation.ShortlistedCurrenciesRoute
import com.osscabinet.ossWallet.ui.navigation.SmartSuggestionsRoute
import com.osscabinet.ossWallet.ui.navigation.AccountListRoute
import com.osscabinet.ossWallet.ui.navigation.CategoryListRoute
import com.osscabinet.ossWallet.ui.navigation.FilterListRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentEntryRoute
import com.osscabinet.ossWallet.ui.navigation.TagListRoute
import com.osscabinet.ossWallet.ui.navigation.TemplateEntryRoute
import com.osscabinet.ossWallet.ui.navigation.TemplateListRoute
import com.osscabinet.ossWallet.ui.screens.openinweb.OpenInWebScreen
import com.osscabinet.ossWallet.ui.screens.openinweb.OpenInWebViewModel
import com.osscabinet.ossWallet.ui.screens.settings.CurrencySettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.DataSettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.DisplaySettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.SecuritySettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.SettingsScreen
import com.osscabinet.ossWallet.ui.screens.settings.ShortlistedCurrenciesScreen
import com.osscabinet.ossWallet.ui.screens.smartsuggestions.SmartSuggestionsScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    vms: SharedNavViewModels
) {
    composable<SettingsRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        SettingsScreen(
            viewModel = vms.settingsViewModel,
            onDisplayClick = { navController.navigate(SettingsDisplayRoute) },
            onCurrencyClick = { navController.navigate(SettingsCurrencyRoute) },
            onDataClick = { navController.navigate(SettingsDataRoute) },
            onSecurityClick = { navController.navigate(SettingsSecurityRoute) },
            onBackClick = { navController.popBackStack() }
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
            onBackClick = { navController.popBackStack() }
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
            onShortlistedCurrenciesClick = { navController.navigate(ShortlistedCurrenciesRoute) },
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<SettingsDataRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        DataSettingsScreen(
            onManageAccountsClick = { navController.navigate(AccountListRoute) },
            onManageTagsClick = { navController.navigate(TagListRoute) },
            onManageCategoriesClick = { navController.navigate(CategoryListRoute) },
            onTemplatesClick = { navController.navigate(TemplateListRoute) },
            onManageFiltersClick = { navController.navigate(FilterListRoute) },
            onSmartSuggestionsClick = { navController.navigate(SmartSuggestionsRoute) },
            onBackClick = { navController.popBackStack() }
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
            onOpenInWebClick = { navController.navigate(OpenInWebRoute) },
            onBackClick = { navController.popBackStack() }
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
            onBack = { navController.popBackStack() }
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
            onAcceptAsTemplate = { navController.navigate(TemplateEntryRoute()) },
            onAcceptAsPlannedPayment = { navController.navigate(PlannedPaymentEntryRoute()) },
            onBackClick = { navController.popBackStack() }
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
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
