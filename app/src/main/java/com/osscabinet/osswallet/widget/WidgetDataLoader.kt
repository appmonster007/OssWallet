package com.osscabinet.ossWallet.widget

import android.content.Context
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

object WidgetDataLoader {
    data class CommonData(
        val profileId: Int,
        val baseCurrency: String,
        val precision: Int,
        val exchangeRates: Map<String, BigDecimal>,
        val databaseQueryService: DatabaseQueryService,
        val categoryRepository: CategoryRepository
    )

    suspend fun loadCommon(context: Context): CommonData {
        val container = (context.applicationContext as OssWalletApp).container
        val settings = container.settingsDataStore
        val profileId = settings.currentProfileId.first()
        val baseCurrency = settings.currency.first()
        val precision = settings.decimalPrecision.first()
        val exchangeRates = container.db.currencyDao().getAllRates()
            .associate { it.currencyCode to BigDecimal.valueOf(it.rate) }

        return CommonData(
            profileId = profileId,
            baseCurrency = baseCurrency,
            precision = precision,
            exchangeRates = exchangeRates,
            databaseQueryService = container.databaseQueryService,
            categoryRepository = container.categoryRepository
        )
    }
}
