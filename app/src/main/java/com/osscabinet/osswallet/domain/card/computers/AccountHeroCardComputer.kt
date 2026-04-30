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
        
        // Calculate total balance from ALL transactions for this account using exchange rates
        val totalBalance = AggregationService.calculateAccountBalance(
            accountId = accountId,
            allTransactions = input.transactions,
            accountCurrency = account.currencyCode,
            exchangeRates = input.exchangeRates
        )
        
        // Calculate income/expense breakdown for the filtered period in account currency
        val breakdown = AggregationService.calculateIncomeExpenseBreakdown(
            transactions = filteredTx,
            exchangeRates = input.exchangeRates,
            baseCurrency = account.currencyCode
        )
        
        // Calculate currency balances for this account (based on filtered transactions or all?)
        // Usually currency balances on a card like this should reflect the overall state, 
        // but since we have a filter, let's see. 
        // If it's like a "hero" card, maybe it should show overall currency balances?
        // Let's stick to filtered transactions for now as it's a "card" which often has a timeframe.
        // Actually, the user said "it should always be calculated from transactions".
        val currencyBalances = AggregationService.calculateTotalByCurrency(
            input.transactions.filter { it.accountId == accountId }
        )

        return CardRenderData.AccountHero(
            config = config,
            account = account,
            balance = totalBalance,
            income = breakdown.totalIncome,
            expense = breakdown.totalExpense,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            currencyBalances = currencyBalances,
            precision = input.precision
        )
    }
}
