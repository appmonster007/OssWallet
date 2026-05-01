package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService
import java.math.BigDecimal

class AccountHeroCardComputer : CardDataComputer<AccountHeroCardConfig, CardRenderData.AccountHero> {
    override val supportedType = CardType.ACCOUNT_HERO

    override fun compute(input: CardInput, config: AccountHeroCardConfig): CardRenderData.AccountHero {
        // For AccountHero, the scope should already be the account.
        // If not, we extract it from input.filter (if it has exactly one account) 
        // or from input.scopeAccountId if we had kept it.
        // Since we removed scopeAccountId, we expect input.filter to contain the account restriction.
        
        val targetAccountId = input.filter.accountIds.firstOrNull() 
            ?: config.filter.accountIds.firstOrNull() 
            ?: error("AccountHeroCard requires an account scope")
            
        val account = input.accounts.find { it.id == targetAccountId } ?: error("Account not found: $targetAccountId")
        
        // Apply base scope
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)
        
        // Apply user filter (period)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)
        
        // Calculate total balance from BASE transactions (Scope Lifetime)
        val totalBalance = AggregationService.calculateAccountBalance(
            accountId = targetAccountId,
            allTransactions = baseTx,
            accountCurrency = account.currencyCode,
            exchangeRates = input.exchangeRates
        )
        
        // Calculate income/expense breakdown for the filtered period
        val breakdown = AggregationService.calculateIncomeExpenseBreakdown(
            transactions = filteredTx,
            exchangeRates = input.exchangeRates,
            baseCurrency = account.currencyCode
        )
        
        // Currency balances within base scope
        val currencyBalances = AggregationService.calculateTotalByCurrency(
            baseTx.filter { it.accountId == targetAccountId }
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
