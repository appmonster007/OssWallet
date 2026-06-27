package com.osscabinet.ossWallet.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val EXCHANGE_RATE_API_BASE_URL = "https://open.er-api.com/"
    private const val FRANKFURTER_API_BASE_URL = "https://api.frankfurter.app/"

    private val logging = HttpLoggingInterceptor().apply {
        level = if (com.osscabinet.ossWallet.core.data.BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val contentType = "application/json".toMediaType()

    // The [json] is the single app-wide instance provided by SerializationModule,
    // so the network path shares the same kotlinx config (incl. the contextual
    // BigDecimalSerializer) as the rest of the app.
    fun exchangeRateApi(json: Json): ExchangeRateApi =
        Retrofit.Builder()
            .baseUrl(EXCHANGE_RATE_API_BASE_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(okHttpClient)
            .build()
            .create(ExchangeRateApi::class.java)

    fun frankfurterApi(json: Json): FrankfurterApi =
        Retrofit.Builder()
            .baseUrl(FRANKFURTER_API_BASE_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(okHttpClient)
            .build()
            .create(FrankfurterApi::class.java)
}
