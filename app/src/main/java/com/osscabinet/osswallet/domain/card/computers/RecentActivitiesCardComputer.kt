package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

class RecentActivitiesCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<RecentActivitiesCardConfig, CardRenderData.RecentActivities> {
    override val supportedType = CardType.RECENT_ACTIVITIES

    override suspend fun compute(input: CardInput, config: RecentActivitiesCardConfig): CardRenderData.RecentActivities {
        val accountNamesMap = input.accounts.associate { it.id to it.name }
        val categoryMap = input.categories.associateBy { it.id }

        val mergedFilter = input.filter.copy(
            accountIds = if (config.filter.accountIds.isNotEmpty())
                input.filter.accountIds.intersect(config.filter.accountIds.toSet()).toList()
            else
                input.filter.accountIds
        )

        val recentTransactions = queryService
            .observeFilteredTransactions(input.profileId, mergedFilter, limit = 20)
            .first()
            .map { TransactionWithBalance(it, BigDecimal.ZERO, BigDecimal.ZERO) }

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
