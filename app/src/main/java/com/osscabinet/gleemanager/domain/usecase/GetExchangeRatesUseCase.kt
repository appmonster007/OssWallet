package com.osscabinet.gleemanager.domain.usecase

import com.osscabinet.gleemanager.domain.repository.CurrencyRepository

class GetExchangeRatesUseCase(
    private val repository: CurrencyRepository
) {
    suspend operator fun invoke(baseCurrency: String = "USD"): Map<String, Double> {
        return repository.getExchangeRates(baseCurrency)
    }
}
