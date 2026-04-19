package com.osscabinet.ossWallet.domain.service

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Domain service for currency-related business logic.
 * Moved from ui.util.CurrencyUtils to follow Clean Architecture principles.
 */
object CurrencyService {
    
    /**
     * Converts an amount from one currency to another using exchange rates.
     * 
     * @param amount The amount to convert
     * @param fromCurrency The source currency code
     * @param toCurrency The target currency code
     * @param exchangeRates Map of currency codes to their exchange rates
     * @param precision Decimal precision for the result
     * @return The converted amount
     */
    fun convertAmount(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 2
    ): BigDecimal {
        if (fromCurrency == toCurrency) return amount.setScale(precision, RoundingMode.HALF_UP)

        // Check if we have both rates in the map
        // Note: The map usually contains rates relative to a base currency (e.g., USD or the user's base)
        val fromRate = exchangeRates[fromCurrency]
        val toRate = exchangeRates[toCurrency]

        if (fromRate == null || toRate == null || fromRate.signum() == 0) {
            // Fallback: If we can't find rates, return original amount or zero to avoid crashing
            return amount.setScale(precision, RoundingMode.HALF_UP)
        }

        // Standard cross-rate conversion: (Amount / RateFromBase) * RateToBase
        val intermediatePrecision = maxOf(precision + 4, 10)
        val inBase = amount.divide(fromRate, intermediatePrecision, RoundingMode.HALF_UP)

        return inBase.multiply(toRate).setScale(precision, RoundingMode.HALF_UP)
    }

    /**
     * Calculates the total amount in a target currency from a map of currency totals.
     * 
     * @param currencyTotals Map of currency codes to their amounts
     * @param targetCurrency The target currency code for the total
     * @param exchangeRates Map of currency codes to their exchange rates
     * @param precision Decimal precision for the result
     * @return The total amount in the target currency
     */
    fun calculateTotalInCurrency(
        currencyTotals: Map<String, BigDecimal>,
        targetCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 2
    ): BigDecimal {
        return currencyTotals.entries.fold(BigDecimal.ZERO) { acc, (curr, amount) ->
            // Use high precision for intermediate conversion to avoid accumulation errors
            acc.add(convertAmount(amount, curr, targetCurrency, exchangeRates, precision + 4))
        }.setScale(precision, RoundingMode.HALF_UP)
    }
}
