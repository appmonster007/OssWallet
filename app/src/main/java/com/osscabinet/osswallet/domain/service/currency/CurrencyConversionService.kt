package com.osscabinet.ossWallet.domain.service.currency

import com.osscabinet.ossWallet.domain.service.FinancialSummary
import java.math.BigDecimal

/**
 * Service for currency conversion operations.
 *
 * **DEPRECATED**: All functionality has been merged into [CurrencyService].
 * Use [CurrencyService.convertToBaseCurrency], [CurrencyService.convertFinancialSummaryToBase],
 * and [CurrencyService.calculateTotalInBaseCurrency] instead.
 *
 * This service will be removed in a future release.
 */
@Deprecated(
    message = "Use CurrencyService instead. All conversion functions are now available there.",
    replaceWith = ReplaceWith("CurrencyService"),
    level = DeprecationLevel.WARNING
)
object CurrencyConversionService {

    /**
     * Converts an amount from source currency to base currency using exchange rates.
     *
     * @param amount Amount to convert
     * @param fromCurrency Source currency code
     * @param toCurrency Target/base currency code
     * @param exchangeRates Map of currency codes to exchange rates (rate is from currency to base currency)
     * @return Converted amount in base currency, or original amount if conversion not possible
     */
    @Deprecated(
        "Use CurrencyService.convertToBaseCurrency instead",
        ReplaceWith("CurrencyService.convertToBaseCurrency(amount, fromCurrency, toCurrency, exchangeRates)")
    )
    fun convertToBaseCurrency(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): BigDecimal {
        if (fromCurrency == toCurrency) {
            return amount
        }
        val rate = exchangeRates[fromCurrency]
        if (rate == null || rate.signum() == 0) return amount

        // Match CurrencyService logic: amount / rate (where rate is BASE -> CURRENCY)
        return amount.divide(rate, 10, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Converts a FinancialSummary from multiple currencies to a single base currency.
     *
     * @param summary Summary to convert (may have mixed currencies)
     * @param currency Currency code of the summary
     * @param baseCurrency Target/base currency code
     * @param exchangeRates Map of currency codes to exchange rates
     * @return Converted FinancialSummary in base currency
     */
    @Deprecated(
        "Use CurrencyService.convertFinancialSummaryToBase instead",
        ReplaceWith("CurrencyService.convertFinancialSummaryToBase(summary, currency, baseCurrency, exchangeRates)")
    )
    fun convertFinancialSummary(
        summary: FinancialSummary,
        currency: String,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary {
        return FinancialSummary(
            totalAmount = convertToBaseCurrency(
                summary.totalAmount,
                currency,
                baseCurrency,
                exchangeRates
            ),
            income = convertToBaseCurrency(summary.income, currency, baseCurrency, exchangeRates),
            expense = convertToBaseCurrency(summary.expense, currency, baseCurrency, exchangeRates),
            transactionCount = summary.transactionCount
        )
    }

    /**
     * Aggregates multiple FinancialSummary objects (from different currencies) into a single
     * summary in a base currency.
     *
     * @param summaries Map of currency code to FinancialSummary
     * @param baseCurrency Target base currency
     * @param exchangeRates Map of currency codes to exchange rates
     * @return Aggregated FinancialSummary in base currency
     */
    @Deprecated(
        "Use CurrencyService.calculateTotalInBaseCurrency instead",
        ReplaceWith("CurrencyService.calculateTotalInBaseCurrency(summaries, baseCurrency, exchangeRates)")
    )
    fun calculateTotalInBaseCurrency(
        summaries: Map<String, FinancialSummary>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary {
        if (summaries.isEmpty()) return FinancialSummary.EMPTY

        return summaries.map { (currency, summary) ->
            convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
        }.reduce { acc, summary ->
            FinancialSummary(
                totalAmount = acc.totalAmount + summary.totalAmount,
                income = acc.income + summary.income,
                expense = acc.expense + summary.expense,
                transactionCount = acc.transactionCount + summary.transactionCount
            )
        }
    }
}
