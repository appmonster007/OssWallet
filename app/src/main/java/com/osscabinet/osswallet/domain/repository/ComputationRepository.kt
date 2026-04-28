package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface ComputationRepository {
    val computationContext: Flow<ComputationContext?>
    val exchangeRates: Flow<Map<String, BigDecimal>>
    val selectedCurrency: Flow<String>
    val hasLoadedExchangeRates: Flow<Boolean>
}
