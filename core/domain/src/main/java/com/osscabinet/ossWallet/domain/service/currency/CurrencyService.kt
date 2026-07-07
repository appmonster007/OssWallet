package com.osscabinet.ossWallet.domain.service.currency

import com.osscabinet.ossWallet.domain.service.currency.CurrencyService.activeSystem
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Domain service for currency conversion and core business logic.
 *
 * ARC-05: the formatting configuration (precision + numbering system) is process-wide
 * mutable state. It must be initialized at app entry by [MainActivity] and at widget
 * entry by `WidgetDataLoader` before any reader executes; the [AtomicReference] holder
 * makes the (precision, system) pair update atomically and visible across threads, so
 * concurrent reads can't observe a precision from one config paired with a system from
 * another mid-write. The reads via the `precision` / `activeSystem` properties continue
 * to work for all existing callers.
 */
object CurrencyService {

    data class FormatConfig(val precision: Int, val system: FormattingSystem)

    private val formatConfig: AtomicReference<FormatConfig> =
        AtomicReference(FormatConfig(precision = 2, system = FormattingSystem.WESTERN))

    /**
     * Atomically update both fields together. Use this instead of writing
     * [precision] and [activeSystem] separately to avoid torn reads where a caller
     * sees the new precision paired with the old system (or vice versa).
     */
    fun setFormatConfig(precision: Int, system: FormattingSystem) {
        formatConfig.set(FormatConfig(precision, system))
    }

    val precision: Int
        get() = formatConfig.get().precision

    val activeSystem: FormattingSystem
        get() = formatConfig.get().system

    enum class SymbolPosition { LEFT, RIGHT }

    /**
     * Formatting system defines the style of number and currency display.
     */
    enum class FormattingSystem(
        val decimalSeparator: Char,
        val groupingSeparator: Char,
        val symbolPosition: SymbolPosition,
        val isIndianGrouping: Boolean = false
    ) {
        WESTERN(
            decimalSeparator = '.',
            groupingSeparator = ',',
            symbolPosition = SymbolPosition.LEFT
        ),
        EUROPEAN(
            decimalSeparator = ',',
            groupingSeparator = '.',
            symbolPosition = SymbolPosition.RIGHT
        ),
        INDIAN(
            decimalSeparator = '.',
            groupingSeparator = ',',
            symbolPosition = SymbolPosition.LEFT,
            isIndianGrouping = true
        ),
        SWISS(
            decimalSeparator = '.',
            groupingSeparator = '\'',
            symbolPosition = SymbolPosition.LEFT
        ),
        FRENCH(
            decimalSeparator = ',',
            groupingSeparator = ' ',
            symbolPosition = SymbolPosition.RIGHT
        ),
        ISO(decimalSeparator = '.', groupingSeparator = ' ', symbolPosition = SymbolPosition.LEFT),
        SOUTH_AMERICAN(
            decimalSeparator = ',',
            groupingSeparator = '.',
            symbolPosition = SymbolPosition.LEFT
        );

        companion object {
            fun fromString(value: String?): FormattingSystem =
                entries.find { it.name.equals(value, ignoreCase = true) } ?: WESTERN
        }
    }

    /**
     * Returns the symbol for a given currency code.
     */
    fun getCurrencySymbol(currencyCode: String?): String {
        if (currencyCode.isNullOrBlank()) return ""
        return try {
            val code = currencyCode.uppercase().trim()
            if (code.length == 3 && code.all { it.isLetter() }) {
                Currency.getInstance(code).getSymbol(Locale.getDefault())
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

    // ==================== Conversion & Logic (ExchangeRateSnapshot) ====================

    /**
     * Converts an amount from one currency to another using a snapshot.
     * The snapshot's anchor is treated as the implicit `1.0` rate.
     * If either source or target rate is missing (and not the anchor), the
     * original amount is returned (rescaled).
     */
    fun convertAmount(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        precision: Int = this.precision
    ): BigDecimal {
        if (fromCurrency == toCurrency) return amount.setScale(precision, RoundingMode.HALF_UP)

        val anchor = exchangeRates.anchor
        val fromRate =
            if (fromCurrency == anchor) BigDecimal.ONE else exchangeRates.rates[fromCurrency]
        val toRate = if (toCurrency == anchor) BigDecimal.ONE else exchangeRates.rates[toCurrency]

        if (fromRate == null || toRate == null || fromRate.signum() == 0) {
            return amount.setScale(precision, RoundingMode.HALF_UP)
        }

        val intermediatePrecision = maxOf(precision + 4, 10)
        val inAnchor = amount.divide(fromRate, intermediatePrecision, RoundingMode.HALF_UP)

        return inAnchor.multiply(toRate).setScale(precision, RoundingMode.HALF_UP)
    }

    fun convertToBaseCurrency(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: ExchangeRateSnapshot
    ): BigDecimal {
        return convertAmount(amount, fromCurrency, toCurrency, exchangeRates, 10)
    }

    fun convertFinancialSummary(
        summary: FinancialSummary,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
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
            transfer = convertAmount(
                summary.transfer,
                fromCurrency,
                toCurrency,
                exchangeRates,
                precision
            ),
            transferIn = convertAmount(
                summary.transferIn,
                fromCurrency,
                toCurrency,
                exchangeRates,
                precision
            ),
            transferOut = convertAmount(
                summary.transferOut,
                fromCurrency,
                toCurrency,
                exchangeRates,
                precision
            ),
            crossCcyTransferIn = convertAmount(
                summary.crossCcyTransferIn,
                fromCurrency,
                toCurrency,
                exchangeRates,
                precision
            ),
            crossCcyTransferOut = convertAmount(
                summary.crossCcyTransferOut,
                fromCurrency,
                toCurrency,
                exchangeRates,
                precision
            ),
            transactionCount = summary.transactionCount
        )
    }

    fun calculateTotalFinancialSummary(
        summaries: Map<String, FinancialSummary>,
        targetCurrency: String,
        exchangeRates: ExchangeRateSnapshot
    ): FinancialSummary {
        if (summaries.isEmpty()) return FinancialSummary.EMPTY

        return summaries.map { (currency, summary) ->
            convertFinancialSummary(summary, currency, targetCurrency, exchangeRates)
        }.reduce { acc, summary ->
            FinancialSummary(
                totalAmount = acc.totalAmount + summary.totalAmount,
                income = acc.income + summary.income,
                expense = acc.expense + summary.expense,
                transfer = acc.transfer + summary.transfer,
                transferIn = acc.transferIn + summary.transferIn,
                transferOut = acc.transferOut + summary.transferOut,
                crossCcyTransferIn = acc.crossCcyTransferIn + summary.crossCcyTransferIn,
                crossCcyTransferOut = acc.crossCcyTransferOut + summary.crossCcyTransferOut,
                transactionCount = acc.transactionCount + summary.transactionCount
            )
        }
    }

    fun convertFinancialSummaryToBase(
        summary: FinancialSummary,
        fromCurrency: String,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot
    ): FinancialSummary =
        convertFinancialSummary(summary, fromCurrency, baseCurrency, exchangeRates)

    fun calculateTotalInBaseCurrency(
        summaries: Map<String, FinancialSummary>,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot
    ): FinancialSummary = calculateTotalFinancialSummary(summaries, baseCurrency, exchangeRates)

    fun calculateTotalInCurrency(
        currencyTotals: Map<String, BigDecimal>,
        targetCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        precision: Int = this.precision
    ): BigDecimal {
        return currencyTotals.entries.fold(BigDecimal.ZERO) { acc, (curr, amount) ->
            acc.add(convertAmount(amount, curr, targetCurrency, exchangeRates, precision + 4))
        }.setScale(precision, RoundingMode.HALF_UP)
    }

    // ==================== Back-compat Map<String, BigDecimal> overloads ====================

    @Deprecated("Pass ExchangeRateSnapshot")
    fun convertAmount(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = this.precision
    ): BigDecimal = convertAmount(
        amount, fromCurrency, toCurrency,
        ExchangeRateSnapshot(anchor = "USD", rates = exchangeRates),
        precision
    )

    @Deprecated("Pass ExchangeRateSnapshot")
    fun convertToBaseCurrency(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): BigDecimal = convertToBaseCurrency(
        amount, fromCurrency, toCurrency,
        ExchangeRateSnapshot(anchor = "USD", rates = exchangeRates)
    )

    @Deprecated("Pass ExchangeRateSnapshot")
    fun convertFinancialSummary(
        summary: FinancialSummary,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 10
    ): FinancialSummary = convertFinancialSummary(
        summary, fromCurrency, toCurrency,
        ExchangeRateSnapshot(anchor = "USD", rates = exchangeRates),
        precision
    )

    @Deprecated("Pass ExchangeRateSnapshot")
    fun calculateTotalFinancialSummary(
        summaries: Map<String, FinancialSummary>,
        targetCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary = calculateTotalFinancialSummary(
        summaries, targetCurrency,
        ExchangeRateSnapshot(anchor = "USD", rates = exchangeRates)
    )

    @Deprecated("Pass ExchangeRateSnapshot")
    fun convertFinancialSummaryToBase(
        summary: FinancialSummary,
        fromCurrency: String,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary = convertFinancialSummaryToBase(
        summary, fromCurrency, baseCurrency,
        ExchangeRateSnapshot(anchor = "USD", rates = exchangeRates)
    )

    @Deprecated("Pass ExchangeRateSnapshot")
    fun calculateTotalInBaseCurrency(
        summaries: Map<String, FinancialSummary>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): FinancialSummary = calculateTotalInBaseCurrency(
        summaries, baseCurrency,
        ExchangeRateSnapshot(anchor = "USD", rates = exchangeRates)
    )

    @Deprecated("Pass ExchangeRateSnapshot")
    fun calculateTotalInCurrency(
        currencyTotals: Map<String, BigDecimal>,
        targetCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = this.precision
    ): BigDecimal = calculateTotalInCurrency(
        currencyTotals, targetCurrency,
        ExchangeRateSnapshot(anchor = "USD", rates = exchangeRates),
        precision
    )

    /**
     * Returns all available currency codes.
     */
    fun getAllCurrencies(): List<String> {
        return Currency.getAvailableCurrencies().map { it.currencyCode }.sorted()
    }
}
