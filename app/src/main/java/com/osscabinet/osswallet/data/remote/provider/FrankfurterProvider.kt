package com.osscabinet.ossWallet.data.remote.provider

import com.osscabinet.ossWallet.data.remote.FrankfurterApi
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider

/**
 * Implementation of [ExchangeRateProvider] using Frankfurter API.
 */
class FrankfurterProvider(
    private val api: FrankfurterApi
) : ExchangeRateProvider {

    override val id: String = "frankfurter"
    override val name: String = "Frankfurter"
    override val attribution: String = "Rates by Frankfurter"

    override fun fetchSupportedCurrencies(): List<CurrencyMetadata> {
        return SupportedCurrencies.entries.map { it.toMetadata() }
    }

    override suspend fun fetchRate(base: String, target: String): Result<Double> {
        if (base == target) return Result.success(1.0)
        return fetchAllRates(base).map { it[target] ?: 1.0 }
    }

    override suspend fun fetchAllRates(base: String): Result<Map<String, Double>> {
        return try {
            val response = api.getLatestRates(base)
            val rates = response.rates.toMutableMap()
            if (!rates.containsKey(base)) {
                rates[base] = 1.0
            }
            Result.success(rates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun convertCurrencyAmount(amount: Double, from: String, to: String): Result<Double> {
        return fetchRate(from, to).map { it * amount }
    }

    /**
     * Hardcoded list of supported currencies for Frankfurter.
     */
    enum class SupportedCurrencies(
        val code: String,
        val currencyName: String,
        val country: String
    ) {
        AUD("AUD", "Australian Dollar", "Australia"),
        BGN("BGN", "Bulgarian Lev", "Bulgaria"),
        BRL("BRL", "Brazilian Real", "Brazil"),
        CAD("CAD", "Canadian Dollar", "Canada"),
        CHF("CHF", "Swiss Franc", "Switzerland"),
        CNY("CNY", "Chinese Renminbi", "China"),
        CZK("CZK", "Czech Koruna", "Czech Republic"),
        DKK("DKK", "Danish Krone", "Denmark"),
        EUR("EUR", "Euro", "European Union"),
        GBP("GBP", "Pound Sterling", "United Kingdom"),
        HKD("HKD", "Hong Kong Dollar", "Hong Kong"),
        HUF("HUF", "Hungarian Forint", "Hungary"),
        IDR("IDR", "Indonesian Rupiah", "Indonesia"),
        ILS("ILS", "Israeli New Shekel", "Israel"),
        INR("INR", "Indian Rupee", "India"),
        ISK("ISK", "Icelandic Króna", "Iceland"),
        JPY("JPY", "Japanese Yen", "Japan"),
        KRW("KRW", "South Korean Won", "South Korea"),
        MXN("MXN", "Mexican Peso", "Mexico"),
        MYR("MYR", "Malaysian Ringgit", "Malaysia"),
        NOK("NOK", "Norwegian Krone", "Norway"),
        NZD("NZD", "New Zealand Dollar", "New Zealand"),
        PHP("PHP", "Philippine Peso", "Philippines"),
        PLN("PLN", "Polish Złoty", "Poland"),
        RON("RON", "Romanian Leu", "Romania"),
        SEK("SEK", "Swedish Krone", "Sweden"),
        SGD("SGD", "Singapore Dollar", "Singapore"),
        THB("THB", "Thai Baht", "Thailand"),
        TRY("TRY", "Turkish Lira", "Turkey"),
        USD("USD", "United States Dollar", "United States"),
        ZAR("ZAR", "South African Rand", "South Africa");

        fun toMetadata(): CurrencyMetadata = CurrencyMetadata(
            code = code,
            name = currencyName,
            country = country
        )
    }
}
