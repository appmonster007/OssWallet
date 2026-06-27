package com.osscabinet.ossWallet.domain.service.currency.provider

/**
 * Registry for all available exchange rate providers.
 */
class ExchangeRateProviderRegistry(
    private val providers: List<ExchangeRateProvider>
) {
    /**
     * Returns the provider with the given ID, or the first one if not found.
     */
    fun getProvider(id: String): ExchangeRateProvider {
        return providers.find { it.id == id } ?: providers.first()
    }

    /**
     * Returns all registered providers.
     */
    fun getAllProviders(): List<ExchangeRateProvider> = providers
}
