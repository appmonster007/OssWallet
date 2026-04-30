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
        val filter = TransactionFilter(timelineRange = config.timelineRange)
        val filteredTx = TransactionFilterService.filter(input.transactions, filter)
        
        val accountBreakdowns = AggregationService.breakdownByAccount(
            transactions = filteredTx,
            accounts = input.accounts,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.baseCurrency
        )
        
        val accountStats = accountBreakdowns.associate { b ->
            val acc = input.accounts.find { it.id == b.key }!!
            val accCurrSummary = b.currencyBreakdown[acc.currencyCode] ?: FinancialSummary.EMPTY
            b.key to AccountSummaryItem(
                account = acc,
                currencyBalances = b.currencyBreakdown.mapValues { it.value.totalAmount },
                balanceInBase = b.totalAmount,
                incomeInAccountCurrency = accCurrSummary.income,
                expenseInAccountCurrency = accCurrSummary.expense
            )
        }
        
        val updatedAccounts = input.accounts.map { account ->
            val b = accountBreakdowns.find { it.key == account.id }
            if (b != null) {
                val totalInAccountCurrency = b.currencyBreakdown.map { (curr, summary) ->
                    CurrencyConversionService.convertToBaseCurrency(summary.totalAmount, curr, account.currencyCode, input.exchangeRates)
                }.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                account.copy(balance = totalInAccountCurrency)
            } else account
        }
        
        return CardRenderData.Accounts(config, updatedAccounts, accountStats, input.baseCurrency, "light", input.precision)
    }
}
