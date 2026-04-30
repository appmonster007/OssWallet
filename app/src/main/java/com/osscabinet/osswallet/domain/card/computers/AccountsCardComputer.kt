package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.service.*
import java.math.BigDecimal

class AccountsCardComputer : CardDataComputer<AccountsCardConfig, CardRenderData.Accounts> {
    override val supportedType = CardType.ACCOUNTS

    override fun compute(input: CardInput, config: AccountsCardConfig): CardRenderData.Accounts {
        // Filter transactions for period-specific stats (income/expense)
        val filteredTx = TransactionFilterService.filter(input.transactions, config.filter)
        
        // Use all transactions for balance calculation
        val allAccountTransactions = input.transactions
        
        val accountBreakdownsForPeriod = AggregationService.breakdownByAccount(
            transactions = filteredTx,
            accounts = input.accounts
        )
        
        val accountStats = input.accounts.associate { acc ->
            val periodSummary = accountBreakdownsForPeriod.find { it.key == acc.id }
            val accCurrSummary = periodSummary?.currencyBreakdown?.get(acc.currencyCode) ?: FinancialSummary.EMPTY
            
            // Current total balance (Lifetime)
            val totalInAccountCurrency = AggregationService.calculateAccountBalance(
                accountId = acc.id,
                allTransactions = allAccountTransactions,
                accountCurrency = acc.currencyCode,
                exchangeRates = input.exchangeRates
            )

            // Current total balance in base currency
            val accountTransactions = allAccountTransactions.filter { it.accountId == acc.id }
            val balanceTotalsByCurrency = AggregationService.calculateTotalByCurrency(accountTransactions)
            val totalInBaseCurrency = CurrencyService.calculateTotalInCurrency(
                currencyTotals = balanceTotalsByCurrency,
                targetCurrency = input.baseCurrency,
                exchangeRates = input.exchangeRates,
                precision = 10
            )

            acc.id to AccountSummaryItem(
                account = acc,
                // These are period-specific currency balances? 
                // Actually, for a card called "Accounts", the mini currency badges usually show overall state.
                // But let's keep them period-specific if filtered, to show "activity by currency".
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
