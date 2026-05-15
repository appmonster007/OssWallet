package com.osscabinet.ossWallet.domain.service.currency

import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

/**
 * Domain service for currency conversion and core business logic.
 */
object CurrencyService {

    // ==================== Conversion & Logic ====================

    /**
     * Converts an amount from one currency to another using exchange rates.
     */
    fun convertAmount(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 2
    ): BigDecimal {
        if (fromCurrency == toCurrency) return amount.setScale(precision, RoundingMode.HALF_UP)

        val fromRate = exchangeRates[fromCurrency]
        val toRate = exchangeRates[toCurrency]

        if (fromRate == null || toRate == null || fromRate.signum() == 0) {
            return amount.setScale(precision, RoundingMode.HALF_UP)
        }

        val intermediatePrecision = maxOf(precision + 4, 10)
        val inBase = amount.divide(fromRate, intermediatePrecision, RoundingMode.HALF_UP)

        return inBase.multiply(toRate).setScale(precision, RoundingMode.HALF_UP)
    }

    /**
     * Converts an amount from source currency to base currency using exchange rates.
     */
    fun convertToBaseCurrency(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): BigDecimal {
        return convertAmount(amount, fromCurrency, toCurrency, exchangeRates, 10)
    }

    /**
     * Converts a FinancialSummary from source currency to another currency.
     */
    fun convertFinancialSummary(
        summary: FinancialSummary,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 10
    ): FinancialSummary {
        return FinancialSummary(
            totalAmount = convertAmount(summary.totalAmount, fromCurrency, toCurrency, exchangeRates, precision),
            income = convertAmount(summary.income, fromCurrency, toCurrency, exchangeRates, precision),
            expense = convertAmount(summary.expense, fromCurrency, toCurrency, exchangeRates, precision),
            transactionCount = summary.transactionCount
        )
    }

    /**
     * Aggregates multiple FinancialSummary objects into a single one in a target currency.
     */
    fun calculateTotalFinancialSummary(
        summaries: Map<String, FinancialSummary>,
        targetCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary {
        if (summaries.isEmpty()) return FinancialSummary.EMPTY

        return summaries.map { (currency, summary) ->
            convertFinancialSummary(summary, currency, targetCurrency, exchangeRates)
        }.reduce { acc, summary ->
            FinancialSummary(
                totalAmount = acc.totalAmount + summary.totalAmount,
                income = acc.income + summary.income,
                expense = acc.expense + summary.expense,
                transactionCount = acc.transactionCount + summary.transactionCount
            )
        }
    }

    /**
     * Converts a FinancialSummary to base currency.
     */
    fun convertFinancialSummaryToBase(
        summary: FinancialSummary,
        fromCurrency: String,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary = convertFinancialSummary(summary, fromCurrency, baseCurrency, exchangeRates)

    /**
     * Aggregates multiple FinancialSummary objects into a single one in base currency.
     */
    fun calculateTotalInBaseCurrency(
        summaries: Map<String, FinancialSummary>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary = calculateTotalFinancialSummary(summaries, baseCurrency, exchangeRates)

    /**
     * Calculates the total amount in a target currency from a map of currency totals.
     */
    fun calculateTotalInCurrency(
        currencyTotals: Map<String, BigDecimal>,
        targetCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 2
    ): BigDecimal {
        return currencyTotals.entries.fold(BigDecimal.ZERO) { acc, (curr, amount) ->
            acc.add(convertAmount(amount, curr, targetCurrency, exchangeRates, precision + 4))
        }.setScale(precision, RoundingMode.HALF_UP)
    }

    /**
     * Returns all available currency codes.
     */
    fun getAllCurrencies(): List<String> {
        return Currency.getAvailableCurrencies().map { it.currencyCode }.sorted()
    }
}
