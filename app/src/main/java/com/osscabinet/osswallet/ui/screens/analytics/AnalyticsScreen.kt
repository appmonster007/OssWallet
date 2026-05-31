package com.osscabinet.ossWallet.ui.screens.analytics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.ui.components.AeonShimmer
import com.osscabinet.ossWallet.ui.components.CardTypeSelectionDialog
import com.osscabinet.ossWallet.ui.components.cards.AccountActions
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.BudgetActions
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.cards.LoanActions
import com.osscabinet.ossWallet.ui.components.cards.PaymentActions
import com.osscabinet.ossWallet.ui.components.cards.TagActions
import com.osscabinet.ossWallet.ui.components.cards.TransactionActions
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit = {},
    isTopLevel: Boolean = false,
    viewModel: AnalyticsViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    templateViewModel: TemplateViewModel,
    settingsViewModel: SettingsViewModel,
    onRecordsClick: (TransactionFilter) -> Unit,
    onAccountClick: (String) -> Unit,
    onNavigateToPlannedPaymentList: () -> Unit = {},
    onNavigateToBudgetList: () -> Unit = {},
    onNavigateToLoanList: () -> Unit = {},
    onNavigateToPlannedPaymentDetail: (Long) -> Unit = {},
    onNavigateToBudgetDetail: (Long) -> Unit = {},
    onNavigateToLoanDetail: (Long) -> Unit = {},
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onSplitTransactionClick: (Long) -> Unit = {},
    cardComputerRegistry: CardComputerRegistry
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cardsConfig by viewModel.cardsConfig.collectAsStateWithLifecycle()
    val cardDataMap by viewModel.cardDataMap.collectAsStateWithLifecycle()
    val screenFilter by viewModel.screenFilter.collectAsStateWithLifecycle()
    val commonFilterEnabled by viewModel.commonFilterEnabled.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(
        initialValue = true
    )

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showAddCardDialog) { showAddCardDialog = false }
    BackHandler(enabled = showSettingsDialog) { showSettingsDialog = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Intelligence", style = AppTextStyles.screenTitle) },
                    navigationIcon = {
                        if (isTopLevel) {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = AppColors.primary
                            )
                        }
                        IconButton(onClick = { showAddCardDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add card",
                                tint = AppColors.primary
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    AeonShimmer(
                        innerPadding = innerPadding,
                        showCashflow = false,
                        showAccounts = false
                    )
                }

                is AnalyticsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(state.message)
                    }
                }

                is AnalyticsUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(AppColors.screenBackground)
                    ) {
                        AeonCardList(
                            cards = cardsConfig.cards,
                            cardDataMap = cardDataMap,
                            cardInput = CardInput(
                                profileId = state.profileId,
                                transactions = state.allTransactions,
                                accounts = state.accounts,
                                categories = state.allCategories,
                                exchangeRates = state.exchangeRates,
                                baseCurrency = state.selectedCurrency,
                                plannedPayments = state.plannedPayments,
                                budgets = state.budgets,
                                loans = state.loans,
                                filter = if (commonFilterEnabled) screenFilter else TransactionFilter(),
                                screenContext = ScreenContext.ANALYTICS,
                                commonFilterEnabled = commonFilterEnabled
                            ),
                            cardComputerRegistry = cardComputerRegistry,
                            cardUpdateHandler = viewModel,
                            onReorder = { viewModel.reorderCards(it) },
                            onDelete = { viewModel.removeCard(it) },
                            onRecordsClick = onRecordsClick,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = AeonTheme.dimensions.screenPadding,
                                end = AeonTheme.dimensions.screenPadding,
                                top = AeonTheme.dimensions.spacingMd,
                                bottom = scrollBottomPadding()
                            ),
                            animateCards = cardAnimationEnabled,
                            currentTheme = currentTheme,
                            filterContext = FilterSelectionContext(
                                accounts = state.accounts,
                                allCategories = state.allCategories,
                                allTags = state.allTags,
                                savedFilters = savedFilters,
                                onSearchTransactions = transactionViewModel::searchTransactions
                            ),
                            callbacks = AeonInteractionCallbacks(
                                accountActions = AccountActions(
                                    onClick = onAccountClick,
                                    onSeeAll = { /* Analytics doesn't have a standard account list link */ }
                                ),
                                budgetActions = BudgetActions(
                                    onClick = onNavigateToBudgetDetail,
                                    onSeeAll = onNavigateToBudgetList
                                ),
                                loanActions = LoanActions(
                                    onClick = onNavigateToLoanDetail,
                                    onSeeAll = onNavigateToLoanList
                                ),
                                transactionActions = TransactionActions(
                                    onClick = onEditTransactionClick,
                                    onClone = { onCloneTransactionClick(it) },
                                    onDelete = { transactionViewModel.quickDeleteTransaction(it) },
                                    onSplit = { onSplitTransactionClick(it) },
                                    onFullSwipeRight = {
                                        transactionViewModel.toggleStatusClearedUncleared(
                                            it
                                        )
                                    }
                                ),
                                paymentActions = PaymentActions(
                                    onClick = onNavigateToPlannedPaymentDetail,
                                    onSeeAll = onNavigateToPlannedPaymentList
                                ),
                                tagActions = TagActions(
                                    onAdd = transactionViewModel::addTag
                                )
                            ),
                            disableFilterSheet = commonFilterEnabled,
                        )
                    }
                }
            }
        }

        // Floating Filter Pill
        if (commonFilterEnabled) {
            (uiState as? AnalyticsUiState.Success)?.let { state ->
                TransactionFilterPill(
                    filter = screenFilter,
                    accounts = nonArchivedAccounts,
                    categories = state.allCategories,
                    allTags = state.allTags,
                    onSearchTransactions = { transactionViewModel.searchTransactions(it) },
                    savedFilters = savedFilters,
                    onFilterEvent = { event ->
                        when (event) {
                            is FilterEvent.Apply -> viewModel.updateScreenFilter(event.filter)
                            is FilterEvent.Reset -> viewModel.updateScreenFilter(TransactionFilter())
                            is FilterEvent.UpdateTimeline -> viewModel.updateScreenFilter(
                                screenFilter.copy(timelineRange = event.range)
                            )

                            else -> {}
                        }
                    },
                    onSaveFilterClick = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showSettingsDialog) {
        Dialog(
            onDismissRequest = { showSettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = screenHeight * 0.85f),
                shape = RoundedCornerShape(32.dp),
                color = AppColors.sheetSurface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Intelligence Settings",
                        style = AppTextStyles.heroAmount.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 28.sp,
                            color = AppColors.contentPrimary.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
                    ) {
                        EntrySwitchRow(
                            label = "Common Filter",
                            subtitle = "Use a single filter for all cards on this screen",
                            checked = commonFilterEnabled,
                            onCheckedChange = { viewModel.toggleCommonFilter(it) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = { showSettingsDialog = false }
                        ) {
                            Text(
                                "Close",
                                style = AppTextStyles.listItemTitle.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCardDialog) {
        CardTypeSelectionDialog(
            screenContext = ScreenContext.ANALYTICS,
            onDismiss = { showAddCardDialog = false },
            onCardTypeSelected = { cardType ->
                viewModel.addCard(cardType)
                showAddCardDialog = false
            }
        )
    }
}
