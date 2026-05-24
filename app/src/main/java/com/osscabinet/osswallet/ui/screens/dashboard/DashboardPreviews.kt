package com.osscabinet.ossWallet.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.flowOf

@Preview(showBackground = true)
@Composable
fun DashboardGlanceTabPreview() {
    val mockState = DashboardUiState.Success(
        profileId = 1,
        accounts = listOf(
            Account(
                id = "1",
                name = "Main Account",
                currencyCode = "USD",
                structureType = AccountStructureType.SAVINGS,
                colorHex = "#4CAF50",
                icon = "AccountBalance",
            )
        ),
        cardDataMap = emptyMap(),
        allTransactions = emptyList(),
        recentActivitiesState = TransactionListState(groups = emptyList(), isEmpty = true),
        allCategories = emptyList(),
        baseCurrency = "USD",
        exchangeRates = emptyMap()
    )

    val sampleCards = listOf(
        NetWorthCardConfig(cardId = "net_worth_1", order = 0)
    )

    OssWalletTheme {
        DashboardGlanceTab(
            state = mockState,
            cardsConfig = DashboardCardsConfig(cards = sampleCards),
            cardComputerRegistry = CardComputerRegistry(emptyList()),
            cardUpdateHandler = object : CardUpdateHandler {
                override val cardUpdateScope: CoroutineScope get() = MainScope()
                override val cardRepository: CardRepository get() = error("Mock")
                override val cardScreenContext: ScreenContext get() = ScreenContext.DASHBOARD
            },
            savedFilters = emptyList(),
            currentTheme = "Light",
            cardAnimationEnabled = true,
            onShowAddCardDialogChange = {},
            onRecordsClick = {},
            onManageAccountsClick = {},
            onAccountClick = {},
            onSaveFilterClick = {},
            onEditTransactionClick = {},
            onCloneTransactionClick = {},
            onSplitTransactionClick = {},
            onSearchTransactions = { flowOf(emptyList()) },
            onItemExpanded = {},
            onItemCollapsed = {},
            onStatusChange = { _, _ -> },
            onDeleteTransaction = {},
            onFullSwipeRight = {},
            onQuickConfirm = {},
            onQuickSkip = {},
            onAddTag = { _, _ -> },
            onReorderCards = {},
            onRemoveCard = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardRecentActivitiesTabPreview() {
    OssWalletTheme {
        DashboardRecentActivitiesTab(
            recentActivitiesState = TransactionListState(groups = emptyList(), isEmpty = true),
            revealedCardIds = emptyList(),
            recentActivitiesBreakdown = true,
            baseCurrency = "USD",
            onRecordsClick = {},
            onTransactionEvent = {}
        )
    }
}
