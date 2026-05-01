package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.components.CardReorderableList
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext

/**
 * Robust, unified card list component for all analytical screens.
 * Standardizes padding, spacing, reordering, and rendering logic.
 */
@Composable
fun AeonCardList(
    cards: List<CardConfig>,
    cardInput: CardInput,
    cardComputerRegistry: CardComputerRegistry,
    cardUpdateHandler: CardUpdateHandler,
    onReorder: (List<CardConfig>) -> Unit,
    onDelete: (String) -> Unit,
    onRecordsClick: (RecordsFilterContext) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    animateCards: Boolean = true,
    currentTheme: String = "light",
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList(),
    onManageAccountsClick: () -> Unit = {},
    onAccountsViewClick: () -> Unit = {},
    onAccountClick: (String) -> Unit = {},
    baseFilter: TransactionFilter = TransactionFilter(),
    forceDisableAccountSelection: Boolean = false
) {
    CardReorderableList(
        items = cards.sortedBy { it.order },
        key = { it.cardId },
        onReorder = onReorder,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        animateCards = animateCards
    ) { cardConfig, isDragging, dragModifier ->
        
        // Pass the card-specific input
        val input = remember(cardInput, cardConfig.filter) {
            cardInput.copy(filter = cardConfig.filter)
        }

        val cardWithCallbacks = rememberCardWithCallbacks(
            config = cardConfig,
            input = input,
            registry = cardComputerRegistry,
            updateHandler = cardUpdateHandler,
            onDelete = onDelete,
            onRecordsClick = onRecordsClick,
            onManageAccountsClick = onManageAccountsClick,
            onAccountsViewClick = onAccountsViewClick,
            onAccountClick = onAccountClick,
            currentTheme = currentTheme
        )

        cardWithCallbacks?.let {
            // Apply screen-specific restrictions (e.g. Account Detail screen)
            val effectiveCard = if (forceDisableAccountSelection) {
                it.copyWithRules(
                    rules = it.filterRules.copy(allowAccountSelection = false),
                    baseFilter = baseFilter
                )
            } else it

            UnifiedCardRenderer(
                cardWithCallbacks = effectiveCard,
                modifier = Modifier.fillMaxWidth(),
                headerModifier = dragModifier,
                showTitle = true,
                currentTheme = currentTheme,
                accounts = accounts,
                allCategories = allCategories,
                savedFilters = savedFilters
            )
        } ?: Box(modifier = Modifier.size(0.dp))
    }
}

/**
 * Helper extension to clone CardWithCallbacks with updated rules or base filters.
 */
private fun CardWithCallbacks.copyWithRules(
    rules: CardFilterRules,
    baseFilter: TransactionFilter
): CardWithCallbacks = when (this) {
    is CardWithCallbacks.NetWorth -> copy(filterRules = rules, baseFilter = baseFilter)
    is CardWithCallbacks.CurrencyBalances -> copy(filterRules = rules, baseFilter = baseFilter)
    is CardWithCallbacks.CashFlow -> copy(filterRules = rules, baseFilter = baseFilter)
    is CardWithCallbacks.Accounts -> copy(filterRules = rules, baseFilter = baseFilter)
    is CardWithCallbacks.CategoryBreakdown -> copy(filterRules = rules, baseFilter = baseFilter)
    is CardWithCallbacks.CategoryTreemap -> copy(filterRules = rules, baseFilter = baseFilter)
    is CardWithCallbacks.AccountHero -> copy(filterRules = rules, baseFilter = baseFilter)
    is CardWithCallbacks.PlannedPayments -> copy(filterRules = rules, baseFilter = baseFilter)
    is CardWithCallbacks.Budgets -> copy(filterRules = rules, baseFilter = baseFilter)
    is CardWithCallbacks.Loans -> copy(filterRules = rules, baseFilter = baseFilter)
}
