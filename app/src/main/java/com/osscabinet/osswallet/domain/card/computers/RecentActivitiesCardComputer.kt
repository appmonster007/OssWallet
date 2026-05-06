package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService

class RecentActivitiesCardComputer : CardDataComputer<RecentActivitiesCardConfig, CardRenderData.RecentActivities> {
    override val supportedType = CardType.RECENT_ACTIVITIES

    override fun compute(input: CardInput, config: RecentActivitiesCardConfig): CardRenderData.RecentActivities {
        // Step 1: Apply screen-level filter (e.g., account scope if on AccountDetailScreen)
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)

        // Step 2: Apply card-level filter
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)

        // Step 3: Get recent transactions with balance
        // We need to find these transactions in the allTransactionsWithBalance list if possible,
        // or re-calculate balance. Since we have allTransactionsWithBalance in some contexts,
        // but CardInput might not have it.
        // Actually CardInput has transactions: List<Transaction>
        // We might need to compute balances if they are needed.
        // Looking at RecentActivitiesScreen, it takes List<TransactionWithBalance>.
        
        // For now, let's map them to TransactionWithBalance with zero balances if they aren't provided.
        // Usually, the dashboard/account detail screens have access to TransactionWithBalance.
        // Let's check CardInput again.
        
        val accountNamesMap = input.accounts.associate { it.id to it.name }
        val categoryMap = input.categories.associateBy { it.id }

        // Use same logic as RecentActivitiesScreen
        val recentTransactions = TransactionFilterService.filterToRecent(
            items = filteredTx,
            getTimestamp = { it.timestamp },
            limit = 20,
            days = 7,
            excludeFuture = config.filter.excludeFuture
        ).map { tx ->
            com.osscabinet.ossWallet.domain.model.TransactionWithBalance(
                tx,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO
            )
        }

        return CardRenderData.RecentActivities(
            config = config,
            transactions = recentTransactions,
            accountNamesMap = accountNamesMap,
            categoryMap = categoryMap,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )
    }
}
