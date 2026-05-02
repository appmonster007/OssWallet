package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService
import java.math.BigDecimal
import java.math.RoundingMode

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

        // Base scope: always exclude future transactions — AccountHero shows lifetime-to-now
        val baseFilter = input.filter.copy(excludeFuture = true)
        val baseTx = TransactionFilterService.filter(input.transactions, baseFilter)

        // User period filter: also enforce no future, so even "AllTime" stays historical
        val periodFilter = config.filter.copy(excludeFuture = true)
        val filteredTx = TransactionFilterService.filter(baseTx, periodFilter)
        
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
        ).mapValues { it.value.setScale(input.precision, RoundingMode.HALF_UP) }

        return CardRenderData.AccountHero(
            config = config,
            account = account,
            balance = totalBalance.setScale(input.precision, RoundingMode.HALF_UP),
            income = breakdown.totalIncome.setScale(input.precision, RoundingMode.HALF_UP),
            expense = breakdown.totalExpense.setScale(input.precision, RoundingMode.HALF_UP),
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            currencyBalances = currencyBalances,
            precision = input.precision
        )
    }
}
