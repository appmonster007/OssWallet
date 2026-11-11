package com.osscabinet.ossWallet.domain.service.currency.provider

/**
 * Standard interface for currency exchange rate providers.
 */
interface ExchangeRateProvider {
    /** Unique identifier for the provider (e.g., "exchangerate-api") */
    val id: String

    /** Human-readable name of the provider */
    val name: String

    /** Attribution text required by the provider's TOS */
    val attribution: String

    /**
     * The currencies this provider supports, from a list bundled with the app. Never touches the network, so it is
     * safe to call before the user has chosen to let the app go online.
     */
    fun bundledCurrencies(): List<CurrencyMetadata>

    /**
     * Returns the list of currencies supported by this provider, asking the provider's server where it offers one.
     */
    suspend fun fetchSupportedCurrencies(): List<CurrencyMetadata>

    /**
     * Fetches the exchange rate between the base and target currency.
     */
    suspend fun fetchRate(base: String, target: String): Result<Double>

    /**
     * Fetches all available exchange rates for the given base currency.
     */
    suspend fun fetchAllRates(base: String): Result<Map<String, Double>>

    /**
     * Converts an amount from one currency to another.
     * Providers may use specific conversion endpoints for better accuracy.
     */
    suspend fun convertCurrencyAmount(amount: Double, from: String, to: String): Result<Double>
}
