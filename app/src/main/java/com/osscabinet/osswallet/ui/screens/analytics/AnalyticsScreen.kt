package com.osscabinet.ossWallet.ui.screens.analytics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.getAtelierTab
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonShimmer
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.cards.AccountActions
import com.osscabinet.ossWallet.ui.components.cards.AeonCardAtelier
import com.osscabinet.ossWallet.ui.components.cards.AeonCardList
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.BudgetActions
import com.osscabinet.ossWallet.ui.components.cards.FilterPillController
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.cards.LoanActions
import com.osscabinet.ossWallet.ui.components.cards.LocalEditingCardId
import com.osscabinet.ossWallet.ui.components.cards.LocalFilterPillController
import com.osscabinet.ossWallet.ui.components.cards.LocalOnEditCard
import com.osscabinet.ossWallet.ui.components.cards.PaymentActions
import com.osscabinet.ossWallet.ui.components.cards.TagActions
import com.osscabinet.ossWallet.ui.components.cards.TransactionActions
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import kotlinx.coroutines.launch

/**
 * A Value Object representing a valid configuration for the Analytics screen.
 */
data class AnalyticsConfig(
    val isTopLevel: Boolean = false
)

/**
 * Grouped actions for the Analytics screen.
 */
data class AnalyticsActions(
    val onMenuClick: () -> Unit,
    val onBackClick: () -> Unit = {},
    val onRecordsClick: (TransactionFilter) -> Unit,
    val onAccountClick: (String) -> Unit,
    val onNavigateToPlannedPaymentList: () -> Unit = {},
    val onNavigateToBudgetList: () -> Unit = {},
    val onNavigateToLoanList: () -> Unit = {},
    val onNavigateToPlannedPaymentDetail: (Long) -> Unit = {},
    val onNavigateToBudgetDetail: (Long) -> Unit = {},
    val onNavigateToLoanDetail: (Long) -> Unit = {},
    val onEditTransactionClick: (Long) -> Unit,
    val onCloneTransactionClick: (Long) -> Unit = {},
    val onSplitTransactionClick: (Long) -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AnalyticsScreen(
    config: AnalyticsConfig,
    actions: AnalyticsActions,
    vms: SharedNavViewModels
) {
    val viewModel = vms.analyticsViewModel
    val accountViewModel = vms.accountViewModel
    val transactionViewModel = vms.transactionViewModel
    val settingsViewModel = vms.settingsViewModel

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cardsConfig by viewModel.cardsConfig.collectAsStateWithLifecycle()
    val cardDataMap by viewModel.cardDataMap.collectAsStateWithLifecycle()
    val screenFilter by viewModel.screenFilter.collectAsStateWithLifecycle()
    val commonFilterEnabled by viewModel.commonFilterEnabled.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val cardAnimationEnabled by settingsViewModel.cardAnimationEnabled.collectAsStateWithLifecycle(
        initialValue = true
    )

    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingCardId by remember { mutableStateOf<String?>(null) }

    val tabs = listOf("STATUS", "WALLET", "TRACK", "TRENDS")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }
    BackHandler(enabled = showSettingsDialog) { showSettingsDialog = false }
    BackHandler(enabled = editingCardId != null) { editingCardId = null }

    val pillHeight = AeonSize.D64.value
    val filterPillController = remember { FilterPillController() }

    AeonHazeProvider { hazeLayers ->
        CompositionLocalProvider(
            LocalFilterPillController provides filterPillController,
            LocalEditingCardId provides editingCardId,
            LocalOnEditCard provides { editingCardId = it }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            // 1. Content Layer (Layer 1)
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
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
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.Top
                            ) { page ->
                                val currentTab = tabs[page]
                                val filteredCards = remember(currentTab, cardsConfig.cards) {
                                    cardsConfig.cards.filter { it.cardType.getAtelierTab() == currentTab }
                                }

                                if (filteredCards.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No ${currentTab.lowercase()} widgets active",
                                            style = AppTextStyles.listItemSubtitle,
                                            color = AppColors.contentSecondary.copy(alpha = 0.5f)
                                        )
                                    }
                                } else {
                                    AeonCardList(
                                        cards = filteredCards,
                                        cardDataMap = cardDataMap,
                                        cardInput = CardInput(
                                            profileId = state.profileId,
                                            transactions = state.allTransactions,
                                            accounts = state.accounts,
                                            categories = state.allCategories,
                                            exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(
                                                anchor = state.selectedCurrency,
                                                rates = state.exchangeRates
                                            ),
                                            targetCurrency = state.selectedCurrency,
                                            plannedPayments = state.plannedPayments,
                                            budgets = state.budgets,
                                            loans = state.loans,
                                            tags = state.allTags,
                                            filter = if (commonFilterEnabled) screenFilter else TransactionFilter(),
                                            screenContext = ScreenContext.ANALYTICS,
                                            commonFilterEnabled = commonFilterEnabled
                                        ),
                                        cardUpdateHandler = viewModel,
                                        onReorder = { reordered ->
                                            val fullList = cardsConfig.cards
                                            val filteredIds = reordered.map { it.cardId }.toSet()
                                            val originalIndices =
                                                fullList.mapIndexedNotNull { index, card ->
                                                    if (card.cardId in filteredIds) index else null
                                                }
                                            val newFullList = fullList.toMutableList()
                                            reordered.forEachIndexed { index, card ->
                                                newFullList[originalIndices[index]] = card
                                            }
                                            viewModel.reorderCards(newFullList)
                                        },
                                        onDelete = { /* Deletion disabled on Analytics */ },
                                        onRecordsClick = actions.onRecordsClick,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            start = AeonTheme.dimensions.screenPadding,
                                            end = AeonTheme.dimensions.screenPadding,
                                            top = AeonTheme.dimensions.spacingMd + pillHeight,
                                            bottom = scrollBottomPadding() + innerPadding.calculateBottomPadding()
                                        ),
                                        animateCards = cardAnimationEnabled,
                                        filterContext = FilterSelectionContext(
                                            accounts = state.accounts,
                                            allCategories = state.allCategories,
                                            allTags = state.allTags,
                                            savedFilters = savedFilters,
                                            onSearchTransactions = transactionViewModel::searchTransactions
                                        ),
                                        callbacks = AeonInteractionCallbacks(
                                            accountActions = AccountActions(
                                                onClick = actions.onAccountClick,
                                                onSeeAll = { /* Analytics doesn't have a standard account list link */ }
                                            ),
                                            budgetActions = BudgetActions(
                                                onClick = actions.onNavigateToBudgetDetail,
                                                onSeeAll = actions.onNavigateToBudgetList
                                            ),
                                            loanActions = LoanActions(
                                                onClick = actions.onNavigateToLoanDetail,
                                                onSeeAll = actions.onNavigateToLoanList
                                            ),
                                            transactionActions = TransactionActions(
                                                onClick = actions.onEditTransactionClick,
                                                onClone = { actions.onCloneTransactionClick(it) },
                                                onDelete = {
                                                    transactionViewModel.quickDeleteTransaction(
                                                        it
                                                    )
                                                },
                                                onSplit = { actions.onSplitTransactionClick(it) },
                                                onJump = { id, availableIds ->
                                                    transactionViewModel.handleJumpRequest(
                                                        id,
                                                        availableIds
                                                    )
                                                },
                                                onFullSwipeRight = {
                                                    transactionViewModel.toggleStatusClearedUncleared(
                                                        it
                                                    )
                                                }
                                            ),
                                            paymentActions = PaymentActions(
                                                onClick = actions.onNavigateToPlannedPaymentDetail,
                                                onSeeAll = actions.onNavigateToPlannedPaymentList
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
                }
            }

            // 2. Top Bar Layer (Layer 3 - Floating Pills)
            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Intelligence",
                            isTopLevel = config.isTopLevel,
                            onMenuClick = if (config.isTopLevel) actions.onMenuClick else null,
                            onBackClick = if (!config.isTopLevel) actions.onBackClick else null,
                            tabs = tabs,
                            selectedTabIndex = pagerState.currentPage,
                            onTabSelected = { index ->
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )
                    }
                ),
                right = AeonTopBarSection(
                    content = {
                        AeonTopBarActionPill {
                            AppBarIconButton(
                                icon = Icons.Default.Settings,
                                onClick = { showSettingsDialog = true }
                            )
                        }
                    }
                )
            )

            // 3. Floating Filter Pill Layer (Layer 4)
            val pillRequest = filterPillController.activeRequest
            if (pillRequest != null) {
                TransactionFilterPill(
                    filter = pillRequest.filter,
                    accounts = pillRequest.accounts,
                    categories = pillRequest.categories,
                    allTags = pillRequest.allTags,
                    savedFilters = pillRequest.savedFilters,
                    onSearchTransactions = pillRequest.onSearchTransactions,
                    onFilterEvent = pillRequest.onFilterEvent,
                    onSaveFilterClick = pillRequest.onSaveFilterClick,
                    onDismiss = {
                        pillRequest.onDismiss()
                        filterPillController.dismiss()
                    },
                    modifier = Modifier.fillMaxSize(),
                    filterRules = pillRequest.filterRules,
                    baseFilter = pillRequest.baseFilter,
                )
            }

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
                                is FilterEvent.Reset -> viewModel.updateScreenFilter(
                                    TransactionFilter()
                                )

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
                        shape = RoundedCornerShape(AeonSize.D32.value),
                        color = AppColors.sheetSurface,
                        tonalElevation = AeonSize.D6.value
                    ) {
                        Column(
                            modifier = Modifier.padding(AeonSize.D24.value)
                        ) {
                            Text(
                                text = "Intelligence Settings",
                                style = AppTextStyles.heroAmount.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 28.sp,
                                    color = AppColors.contentPrimary.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier.padding(bottom = AeonSize.D24.value)
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
                                    onCheckedChange = { viewModel.toggleCommonFilter(it) },
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
        }
    }
}
}
