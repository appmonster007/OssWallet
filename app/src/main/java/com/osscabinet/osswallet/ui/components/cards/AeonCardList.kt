package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

        // cardInput.filter is the screen's base scope. Card computers apply config.filter on top
        // of that in their own two-step logic — do NOT overwrite input.filter here.
        val cardWithCallbacks = rememberCardWithCallbacks(
            config = cardConfig,
            input = cardInput,
            registry = cardComputerRegistry,
            updateHandler = cardUpdateHandler,
            onDelete = onDelete,
            onRecordsClick = onRecordsClick,
            onManageAccountsClick = onManageAccountsClick,
            onAccountsViewClick = onAccountsViewClick,
            onAccountClick = onAccountClick,
            currentTheme = currentTheme
        )

        when (val card = cardWithCallbacks) {
            is CardWithCallbacks.ComputeError -> CardErrorState(card.message)
            null -> Box(modifier = Modifier.size(0.dp))
            else -> {
                val effectiveCard = if (forceDisableAccountSelection) {
                    card.copyWithRules(
                        rules = card.filterRules.copy(allowAccountSelection = false),
                        baseFilter = baseFilter
                    )
                } else card

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
            }
        }
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
    is CardWithCallbacks.ComputeError -> this
}

@Composable
private fun CardErrorState(message: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message ?: "Failed to load card",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
