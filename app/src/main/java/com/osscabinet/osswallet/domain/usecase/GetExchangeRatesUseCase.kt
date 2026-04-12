package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.repository.CurrencyRepository

class GetExchangeRatesUseCase(
    private val repository: CurrencyRepository
) {
    suspend operator fun invoke(baseCurrency: String = "USD"): Map<String, Double> {
        return repository.getExchangeRates(baseCurrency)
    }
}
