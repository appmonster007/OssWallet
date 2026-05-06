package com.osscabinet.ossWallet.domain.service.currency

import com.osscabinet.ossWallet.domain.service.FinancialSummary
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

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
     * Assumes rates are stored as BASE -> CURRENCY (how much base currency equals 1 unit of target).
     *
     * @param amount Amount to convert
     * @param fromCurrency Source currency code
     * @param toCurrency Target/base currency code
     * @param exchangeRates Map of currency codes to exchange rates (rate is BASE -> CURRENCY)
     * @return Converted amount in base currency, or original amount if conversion not possible
     */
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

        // amount / rate (where rate is BASE -> CURRENCY)
        return amount.divide(rate, 10, RoundingMode.HALF_UP)
    }

    /**
     * Converts a FinancialSummary from one currency to another currency.
     *
     * @param summary Summary to convert
     * @param fromCurrency Source currency code
     * @param toCurrency Target/base currency code
     * @param exchangeRates Map of currency codes to exchange rates
     * @param precision Decimal precision for the result (default: 10 for intermediate calculations)
     * @return Converted FinancialSummary in target currency
     */
    fun convertFinancialSummary(
        summary: FinancialSummary,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 10
    ): FinancialSummary {
        return FinancialSummary(
            totalAmount = convertAmount(
                summary.totalAmount,
                fromCurrency,
                toCurrency,
                exchangeRates,
                precision
            ),
            income = convertAmount(
                summary.income,
                fromCurrency,
                toCurrency,
                exchangeRates,
                precision
            ),
            expense = convertAmount(
                summary.expense,
                fromCurrency,
                toCurrency,
                exchangeRates,
                precision
            ),
            transactionCount = summary.transactionCount
        )
    }

    /**
     * Converts a FinancialSummary using convertToBaseCurrency (for BASE->CURRENCY rate storage).
     *
     * @param summary Summary to convert
     * @param fromCurrency Source currency code
     * @param baseCurrency Target base currency code
     * @param exchangeRates Map of currency codes to exchange rates (BASE -> CURRENCY)
     * @return Converted FinancialSummary in base currency
     */
    fun convertFinancialSummaryToBase(
        summary: FinancialSummary,
        fromCurrency: String,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary {
        return FinancialSummary(
            totalAmount = convertToBaseCurrency(
                summary.totalAmount,
                fromCurrency,
                baseCurrency,
                exchangeRates
            ),
            income = convertToBaseCurrency(
                summary.income,
                fromCurrency,
                baseCurrency,
                exchangeRates
            ),
            expense = convertToBaseCurrency(
                summary.expense,
                fromCurrency,
                baseCurrency,
                exchangeRates
            ),
            transactionCount = summary.transactionCount
        )
    }

    /**
     * Aggregates multiple FinancialSummary objects (from different currencies) into a single
     * summary in a base currency using convertToBaseCurrency.
     *
     * @param summaries Map of currency code to FinancialSummary
     * @param baseCurrency Target base currency
     * @param exchangeRates Map of currency codes to exchange rates (BASE -> CURRENCY)
     * @return Aggregated FinancialSummary in base currency
     */
    fun calculateTotalInBaseCurrency(
        summaries: Map<String, FinancialSummary>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary {
        if (summaries.isEmpty()) return FinancialSummary.EMPTY

        return summaries.map { (currency, summary) ->
            convertFinancialSummaryToBase(summary, currency, baseCurrency, exchangeRates)
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
     * Formats an amount with localized grouping and decimal separators.
     */
    fun formatAmount(amount: BigDecimal, precision: Int = 2): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val pattern = buildString {
            append("#,##0")
            if (precision > 0) {
                append(".")
                repeat(precision) { append("0") }
            }
        }
        val format = DecimalFormat(pattern, symbols)
        return format.format(amount)
    }

    /**
     * Returns the symbol for a given currency code (e.g., "$", "€").
     */
    fun getCurrencySymbol(currencyCode: String?): String {
        if (currencyCode.isNullOrBlank()) return ""
        return try {
            val code = currencyCode.uppercase().trim()
            if (code.length == 3 && code.all { it.isLetter() }) {
                Currency.getInstance(code).symbol
            } else {
                currencyCode
            }
        } catch (e: Exception) {
            currencyCode
        }
    }

    /**
     * Returns the full name for a given currency code.
     */
    fun getCurrencyName(currencyCode: String?): String {
        if (currencyCode.isNullOrBlank()) return ""
        return try {
            val code = currencyCode.uppercase().trim()
            if (code.length == 3 && code.all { it.isLetter() }) {
                Currency.getInstance(code).getDisplayName(Locale.getDefault())
            } else {
                currencyCode
            }
        } catch (e: Exception) {
            currencyCode
        }
    }

    /**
     * Shortens large numbers for UI display (e.g., 1000 -> 1K, 1000000 -> 1M)
     */
    fun formatShortNumber(value: BigDecimal): String {
        val absValue = value.abs()
        return when {
            absValue >= BigDecimal("1000000000") -> {
                val short = value.divide(BigDecimal("1000000000"), 1, RoundingMode.HALF_UP)
                "${short.stripTrailingZeros().toPlainString()}B"
            }

            absValue >= BigDecimal("1000000") -> {
                val short = value.divide(BigDecimal("1000000"), 1, RoundingMode.HALF_UP)
                "${short.stripTrailingZeros().toPlainString()}M"
            }

            absValue >= BigDecimal("1000") -> {
                val short = value.divide(BigDecimal("1000"), 1, RoundingMode.HALF_UP)
                "${short.stripTrailingZeros().toPlainString()}K"
            }

            else -> value.setScale(0, RoundingMode.HALF_UP).toString()
        }
    }

    /**
     * Formats a currency amount with the given currency code and precision
     */
    fun format(amount: BigDecimal, currency: String, precision: Int): String {
        val formattedAmount = formatAmount(amount, precision)
        return "$formattedAmount $currency"
    }

    fun formatRight(amount: BigDecimal, currency: String, precision: Int): String {
        val formattedAmount = formatAmount(amount, precision)
        val symbol = getCurrencySymbol(currency)
        return "$formattedAmount $symbol"
    }

    fun formatLeft(amount: BigDecimal, currency: String, precision: Int): String {
        val formattedAmount = formatAmount(amount, precision)
        val symbol = getCurrencySymbol(currency)
        return "$symbol$formattedAmount"
    }

    fun getAllCurrencies(): List<String> {
        return Currency.getAvailableCurrencies().map { it.currencyCode }.sorted()
    }
}
