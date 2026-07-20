package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.runtime.Stable
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.screens.categories.CategoryViewModel
import com.osscabinet.ossWallet.ui.screens.dashboard.DashboardViewModel
import com.osscabinet.ossWallet.ui.screens.main.MainViewModel
import com.osscabinet.ossWallet.ui.screens.settings.CurrencyViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.sharedwallet.SharedWalletViewModel
import com.osscabinet.ossWallet.ui.screens.tags.TagViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel

/** Groups the 11 Activity-scoped ViewModels so they can be threaded through NavGraphBuilder extensions as a single parameter. */
@Stable
data class SharedNavViewModels(
    val settingsViewModel: SettingsViewModel,
    val analyticsViewModel: AnalyticsViewModel,
    val categoryViewModel: CategoryViewModel,
    val transactionViewModel: TransactionViewModel,
    val accountViewModel: AccountViewModel,
    val sharedWalletViewModel: SharedWalletViewModel,
    val templateViewModel: TemplateViewModel,
    val currencyViewModel: CurrencyViewModel,
    val dashboardViewModel: DashboardViewModel,
    val tagViewModel: TagViewModel,
    val mainViewModel: MainViewModel
)
