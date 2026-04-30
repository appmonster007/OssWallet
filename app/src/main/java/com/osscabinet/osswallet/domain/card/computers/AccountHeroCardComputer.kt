package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService
import java.math.BigDecimal

class AccountHeroCardComputer : CardDataComputer<AccountHeroCardConfig, CardRenderData.AccountHero> {
    override val supportedType = CardType.ACCOUNT_HERO

    override fun compute(input: CardInput, config: AccountHeroCardConfig): CardRenderData.AccountHero {
        val accountId = input.scopeAccountId ?: error("AccountHeroCard requires a scopeAccountId")
        val account = input.accounts.find { it.id == accountId } ?: error("Account not found: $accountId")
        
        // Ensure filter targets this specific account
        val effectiveFilter = config.filter.copy(accountIds = listOf(accountId))
        val filteredTx = TransactionFilterService.filter(input.transactions, effectiveFilter)
        
        // Calculate net balance for the filtered period in account currency
        val breakdown = AggregationService.calculateIncomeExpenseBreakdown(
            transactions = filteredTx,
            exchangeRates = input.exchangeRates,
            baseCurrency = account.currencyCode
        )
        
        // The net balance in account currency for the period
        val netBalance = breakdown.netFlow

        return CardRenderData.AccountHero(
            config = config,
            account = account,
            balance = account.balance,
            netBalance = netBalance,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            precision = input.precision
        )
    }
}
