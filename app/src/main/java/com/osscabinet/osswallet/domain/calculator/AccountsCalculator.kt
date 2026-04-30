package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.CurrencyConversionService
import com.osscabinet.ossWallet.domain.service.FinancialSummary
import com.osscabinet.ossWallet.domain.service.TransactionFilterService
import java.math.BigDecimal

class AccountsCalculator : CardCalculator<AccountsCardConfig, CardRenderData.Accounts> {
    override fun compute(
        config: AccountsCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        context: ComputationContext
    ): CardRenderData.Accounts {
        val filter = TransactionFilter(timelineRange = config.timelineRange)
        val filteredTx = TransactionFilterService.filter(transactions, filter)
        
        val accountBreakdowns = AggregationService.breakdownByAccount(
            transactions = filteredTx,
            accounts = accounts,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val accountStats = accountBreakdowns.associate { b ->
            val acc = accounts.find { it.id == b.key }!!
            val accCurrSummary = b.currencyBreakdown[acc.currencyCode] ?: FinancialSummary.EMPTY
            b.key to AccountSummaryItem(
                account = acc,
                currencyBalances = b.currencyBreakdown.mapValues { it.value.totalAmount },
                balanceInBase = b.totalAmount,
                incomeInAccountCurrency = accCurrSummary.income,
                expenseInAccountCurrency = accCurrSummary.expense
            )
        }
        
        val updatedAccounts = accounts.map { account ->
            val b = accountBreakdowns.find { it.key == account.id }
            if (b != null) {
                val totalInAccountCurrency = b.currencyBreakdown.map { (curr, summary) ->
                    CurrencyConversionService.convertToBaseCurrency(summary.totalAmount, curr, account.currencyCode, context.exchangeRates)
                }.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                account.copy(balance = totalInAccountCurrency)
            } else account
        }
        
        return CardRenderData.Accounts(updatedAccounts, accountStats)
    }
}
