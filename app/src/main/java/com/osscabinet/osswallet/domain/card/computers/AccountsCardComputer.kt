package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.service.FinancialSummary
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import java.math.RoundingMode

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
            val currSummary =
                summary?.currencyBreakdown?.get(acc.currencyCode) ?: FinancialSummary.EMPTY

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
                precision = input.precision
            )

            acc.id to AccountSummaryItem(
                account = acc,
                currencyBalances = summary?.currencyBreakdown?.mapValues {
                    it.value.totalAmount.setScale(input.precision, RoundingMode.HALF_UP)
                } ?: emptyMap(),
                balance = balanceInAccountCurrency.setScale(input.precision, RoundingMode.HALF_UP),
                balanceInBase = balanceInBase,
                incomeInAccountCurrency = currSummary.income.setScale(
                    input.precision,
                    RoundingMode.HALF_UP
                ),
                expenseInAccountCurrency = currSummary.expense.setScale(
                    input.precision,
                    RoundingMode.HALF_UP
                )
            )
        }

        return CardRenderData.Accounts(
            config,
            input.accounts,
            accountStats,
            input.baseCurrency,
            "light",
            input.precision
        )
    }
}
