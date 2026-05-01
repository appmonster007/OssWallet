package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.service.*

class AccountsCardComputer : CardDataComputer<AccountsCardConfig, CardRenderData.Accounts> {
    override val supportedType = CardType.ACCOUNTS

    override fun compute(input: CardInput, config: AccountsCardConfig): CardRenderData.Accounts {
        // Base scope
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)
        
        // Final filter for period-specific stats
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)
        
        val accountBreakdownsForPeriod = AggregationService.breakdownByAccount(
            transactions = filteredTx,
            accounts = input.accounts
        )
        
        val accountStats = input.accounts.associate { acc ->
            val periodSummary = accountBreakdownsForPeriod.find { it.key == acc.id }
            val accCurrSummary = periodSummary?.currencyBreakdown?.get(acc.currencyCode) ?: FinancialSummary.EMPTY
            
            // Current total balance (Lifetime within scope)
            val totalInAccountCurrency = AggregationService.calculateAccountBalance(
                accountId = acc.id,
                allTransactions = baseTx,
                accountCurrency = acc.currencyCode,
                exchangeRates = input.exchangeRates
            )

            // Current total balance in base currency
            val accountTransactions = baseTx.filter { it.accountId == acc.id }
            val balanceTotalsByCurrency = AggregationService.calculateTotalByCurrency(accountTransactions)
            val totalInBaseCurrency = CurrencyService.calculateTotalInCurrency(
                currencyTotals = balanceTotalsByCurrency,
                targetCurrency = input.baseCurrency,
                exchangeRates = input.exchangeRates,
                precision = 10
            )

            acc.id to AccountSummaryItem(
                account = acc,
                currencyBalances = periodSummary?.currencyBreakdown?.mapValues { it.value.totalAmount } ?: emptyMap(),
                balance = totalInAccountCurrency,
                balanceInBase = totalInBaseCurrency,
                incomeInAccountCurrency = accCurrSummary.income,
                expenseInAccountCurrency = accCurrSummary.expense
            )
        }
        
        return CardRenderData.Accounts(config, input.accounts, accountStats, input.baseCurrency, "light", input.precision)
    }
}
