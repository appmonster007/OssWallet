package com.osscabinet.ossWallet.widget

import android.content.Context
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

object WidgetDataLoader {
    data class CommonData(
        val profileId: Int,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val databaseQueryService: DatabaseQueryService,
        val categoryRepository: CategoryRepository,
        val accountRepository: AccountRepository
    )

    suspend fun loadCommon(context: Context): CommonData {
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDataLoaderEntryPoint::class.java
        )
        val settings = ep.settingsDataStore()
        val profileId = settings.currentProfileId.first()
        val baseCurrency = settings.currency.first()
        val precision = settings.decimalPrecision.first()

        // ARC-05: atomic paired update for the widget process. Preserves the existing
        // numbering system; widgets don't currently know the user's system preference,
        // so we keep whatever the AtomicReference already holds.
        CurrencyService.setFormatConfig(precision = precision, system = CurrencyService.activeSystem)
        TimelineService.precision = precision

        val exchangeRates = ep.db().currencyDao().getAllRates()
            .associate { it.currencyCode to it.rate.toBigDecimal() }

        return CommonData(
            profileId = profileId,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            databaseQueryService = ep.databaseQueryService(),
            categoryRepository = ep.categoryRepository(),
            accountRepository = ep.accountRepository()
        )
    }
}
