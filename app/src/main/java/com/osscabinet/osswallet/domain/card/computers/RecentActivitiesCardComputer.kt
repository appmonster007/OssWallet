package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.math.RoundingMode

class RecentActivitiesCardComputer : CardDataComputer<RecentActivitiesCardConfig, CardRenderData.RecentActivities> {
    override val supportedType = CardType.RECENT_ACTIVITIES

    override suspend fun compute(input: CardInput, config: RecentActivitiesCardConfig): CardRenderData.RecentActivities {
        val mergedFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)

        // Compute running balances for the specific account(s) involved in the filter.
        // If no accounts are filtered, default to all active (non-archived) accounts.
        val targetAccountIds = mergedFilter.accountIds.ifEmpty { input.effectiveAccountIds }

        val allTransactions = input.transactionsFor(mergedFilter)
            .asSequence()
            .sortedBy { it.timestamp }
            .groupBy { it.accountId }
            .flatMap { (accId, accountTransactions) ->
                val account = input.accounts.find { it.id == accId }
                val accountCurrency = account?.currencyCode ?: "USD"

                var runningBalance = BigDecimal.ZERO
                accountTransactions.map { transaction ->
                    val txAmountInAccountCurrency = CurrencyService.convertAmount(
                        amount = transaction.amount,
                        fromCurrency = transaction.currency,
                        toCurrency = accountCurrency,
                        exchangeRates = input.exchangeRates
                    )

                    val balanceBefore = runningBalance
                    runningBalance = runningBalance.add(txAmountInAccountCurrency)
                    TransactionWithBalance(
                        transaction = transaction,
                        balanceBefore = balanceBefore.setScale(
                            CurrencyService.precision,
                            RoundingMode.HALF_UP
                        ),
                        balanceAfter = runningBalance.setScale(
                            CurrencyService.precision,
                            RoundingMode.HALF_UP
                        )
                    )
                }
            }
            .filter { it.transaction.accountId in targetAccountIds }
            .toList()

        // Filter the pre-computed list for display
        val filteredTransactions = allTransactions
            .filter { txWithBalance ->
                com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService.matches(
                    txWithBalance.transaction,
                    mergedFilter
                )
            }
            .sortedByDescending { it.transaction.timestamp }
            .take(20)
            .toList()

        val groups = TimelineService.getGroupedData(
            transactions = filteredTransactions,
            absoluteHistory = allTransactions,
            groupingType = config.groupBy,
            accounts = input.accounts,
            categories = input.categories,
            allTags = input.tags,
            baseCurrency = input.displayCurrency,
            exchangeRates = input.exchangeRates,
            allTxnBreakdown = config.allTxnBreakdown,
        )

        return CardRenderData.RecentActivities(
            config = config,
            listState = TransactionListState(groups = groups, isEmpty = groups.isEmpty()),
            baseCurrency = input.displayCurrency,
            exchangeRates = input.exchangeRates
        )
    }
}
