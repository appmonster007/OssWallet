package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.service.CurrencyConversionService
import java.math.BigDecimal

class AccountHeroCardComputer : CardDataComputer<AccountHeroCardConfig, CardRenderData.AccountHero> {
    override val supportedType = CardType.ACCOUNT_HERO

    override fun compute(input: CardInput, config: AccountHeroCardConfig): CardRenderData.AccountHero {
        val accountId = input.scopeAccountId ?: error("AccountHeroCard requires a scopeAccountId")
        val account = input.accounts.find { it.id == accountId } ?: error("Account not found: $accountId")
        
        // Simplified net balance for hero card (usually just current balance if no timeline filter)
        // In some contexts, it might be filtered by timeline.
        val netBalance = account.balance 

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
