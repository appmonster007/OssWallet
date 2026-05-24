package com.osscabinet.ossWallet.ui.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.navigation.EnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.ExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.FilterEntryRoute
import com.osscabinet.ossWallet.ui.navigation.FilterListRoute
import com.osscabinet.ossWallet.ui.navigation.PlannedPaymentEntryRoute
import com.osscabinet.ossWallet.ui.navigation.PopEnterTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.PopExitTransitionSpec
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.navigation.SharedWalletRoute
import com.osscabinet.ossWallet.ui.navigation.TagListRoute
import com.osscabinet.ossWallet.ui.navigation.CategoryListRoute
import com.osscabinet.ossWallet.ui.navigation.TemplateEntryRoute
import com.osscabinet.ossWallet.ui.navigation.TemplateListRoute
import com.osscabinet.ossWallet.ui.navigation.TransactionEntryRoute
import com.osscabinet.ossWallet.ui.screens.categories.list.CategoryListScreen
import com.osscabinet.ossWallet.ui.screens.filters.FilterViewModel
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryScreen
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryViewModel
import com.osscabinet.ossWallet.ui.screens.filters.list.FilterListScreen
import com.osscabinet.ossWallet.ui.screens.sharedwallet.SharedWalletScreen
import com.osscabinet.ossWallet.ui.screens.tags.list.TagListScreen
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryScreen
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryViewModel
import com.osscabinet.ossWallet.ui.screens.templates.list.TemplateListScreen
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryScreen
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryViewModel

fun NavGraphBuilder.transactionsGraph(
    navController: NavHostController,
    vms: SharedNavViewModels
) {
    composable<TransactionEntryRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<TransactionEntryRoute>()
        val prefillDto by vms.transactionViewModel.prefillDto.collectAsStateWithLifecycle()
        val entryViewModel: TransactionEntryViewModel = hiltViewModel()
        TransactionEntryScreen(
            transactionId = route.transactionId.takeIf { it != -1L },
            accountId = route.accountId,
            initialType = if (route.transactionType == -1) null else TransactionType.fromInt(route.transactionType),
            initialTemplateId = route.templateId.takeIf { it != -1L },
            prefillDto = prefillDto,
            viewModel = entryViewModel,
            onBackClick = { navController.popBackStack() },
            onDispose = { vms.transactionViewModel.clearPrefillDto() }
        )
    }

    composable<SharedWalletRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        SharedWalletScreen(
            viewModel = vms.sharedWalletViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<TemplateListRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        val categories by vms.transactionViewModel.categories.collectAsStateWithLifecycle()
        val categoryMap = androidx.compose.runtime.remember(categories) { categories.associateBy { it.id } }
        TemplateListScreen(
            viewModel = vms.templateViewModel,
            accountViewModel = vms.accountViewModel,
            categoryMap = categoryMap,
            onAddTemplateClick = { id ->
                navController.navigate(TemplateEntryRoute(templateId = id ?: -1L))
            },
            onCreatePlannedPayment = { template ->
                vms.transactionViewModel.setPlannedPaymentPrefillFromTemplate(template)
                navController.navigate(PlannedPaymentEntryRoute())
            },
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<TemplateEntryRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<TemplateEntryRoute>()
        val prefill by vms.transactionViewModel.templatePrefill.collectAsStateWithLifecycle()
        val entryViewModel: TemplateEntryViewModel = hiltViewModel()
        LaunchedEffect(prefill) {
            if (prefill != null) vms.transactionViewModel.clearTemplatePrefill()
        }
        TemplateEntryScreen(
            templateId = route.templateId.takeIf { it != -1L },
            prefill = prefill,
            viewModel = entryViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<FilterListRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        val filterViewModel: FilterViewModel = hiltViewModel()
        FilterListScreen(
            viewModel = filterViewModel,
            onBackClick = { navController.popBackStack() },
            onAddFilterClick = { navController.navigate(FilterEntryRoute()) },
            onEditFilterClick = { filter ->
                navController.navigate(FilterEntryRoute(filterId = filter.id))
            }
        )
    }

    composable<FilterEntryRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<FilterEntryRoute>()
        val entryViewModel: FilterEntryViewModel = hiltViewModel()
        FilterEntryScreen(
            filterId = route.filterId.takeIf { it != -1L },
            initialFilterJson = route.initialFilterJson,
            viewModel = entryViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<TagListRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        TagListScreen(
            viewModel = vms.tagViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable<CategoryListRoute>(
        enterTransition = { EnterTransitionSpec },
        exitTransition = { ExitTransitionSpec },
        popEnterTransition = { PopEnterTransitionSpec },
        popExitTransition = { PopExitTransitionSpec }
    ) {
        CategoryListScreen(
            viewModel = vms.categoryViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }
}
