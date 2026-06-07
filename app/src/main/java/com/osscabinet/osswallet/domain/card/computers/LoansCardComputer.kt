package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.LoanService

class LoansCardComputer : CardDataComputer<LoansCardConfig, CardRenderData.Loans> {
    override val supportedType = CardType.LOANS

    override suspend fun compute(input: CardInput, config: LoansCardConfig): CardRenderData.Loans {
        val mergedFilter = resolveEffectiveFilter(
            input.filter,
            config.filter,
            config.filterRules,
            input.screenOwnsTimeline
        )
        val now = System.currentTimeMillis()

        val baseFiltered = when (config.showType) {
            "LENT" -> input.loans.filter { it.loanType == LoanType.LENT }
            "BORROWED" -> input.loans.filter { it.loanType == LoanType.BORROWED }
            else -> input.loans
        }

        val filtered = baseFiltered.filter { loan ->
            (mergedFilter.accountIds.isEmpty() || loan.linkedAccountId.isBlank() || loan.linkedAccountId in mergedFilter.accountIds) &&
                    (mergedFilter.currencies.isEmpty() || loan.currency in mergedFilter.currencies)
        }

        val effectiveFilter = mergedFilter.copy(
            accountIds = mergedFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )
        val allTransactions = input.allProfileTransactions.ifEmpty { input.transactions }
        val filteredTx = TransactionFilterService.filter(allTransactions, effectiveFilter)

        val items = filtered.take(config.maxItems).map { loan ->
            LoanService.buildSummaryFromTransactions(
                loan = loan,
                transactions = filteredTx,
                exchangeRates = input.exchangeRates,
                now = now,
                allCategories = input.categories
            )
        }
        return CardRenderData.Loans(config = config, items = items)
    }
}
