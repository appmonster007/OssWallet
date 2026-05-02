package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.service.*

class AccountsCardComputer : CardDataComputer<AccountsCardConfig, CardRenderData.Accounts> {
    override val supportedType = CardType.ACCOUNTS

    override fun compute(input: CardInput, config: AccountsCardConfig): CardRenderData.Accounts {
        // Step 1: Apply base scope filter (screen-level: profile, account group, etc.)
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)

        // Step 2: Apply card config filter (user-selected: timeline, accounts, categories)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)

        // Step 3: Compute all values from filteredTx
        val accountBreakdowns = AggregationService.breakdownByAccount(
            transactions = filteredTx,
            accounts = input.accounts
        )

        val accountStats = input.accounts.associate { acc ->
            val summary = accountBreakdowns.find { it.key == acc.id }
            val currSummary = summary?.currencyBreakdown?.get(acc.currencyCode) ?: FinancialSummary.EMPTY

            val balanceInAccountCurrency = AggregationService.calculateAccountBalance(
                accountId = acc.id,
                allTransactions = filteredTx,
                accountCurrency = acc.currencyCode,
                exchangeRates = input.exchangeRates
            )
            val accountFilteredTxs = filteredTx.filter { it.accountId == acc.id }
            val balanceInBase = CurrencyService.calculateTotalInCurrency(
                currencyTotals = AggregationService.calculateTotalByCurrency(accountFilteredTxs),
                targetCurrency = input.baseCurrency,
                exchangeRates = input.exchangeRates,
                precision = 10
            )

            acc.id to AccountSummaryItem(
                account = acc,
                currencyBalances = summary?.currencyBreakdown?.mapValues { it.value.totalAmount } ?: emptyMap(),
                balance = balanceInAccountCurrency,
                balanceInBase = balanceInBase,
                incomeInAccountCurrency = currSummary.income,
                expenseInAccountCurrency = currSummary.expense
            )
        }

        return CardRenderData.Accounts(config, input.accounts, accountStats, input.baseCurrency, "light", input.precision)
    }
}
