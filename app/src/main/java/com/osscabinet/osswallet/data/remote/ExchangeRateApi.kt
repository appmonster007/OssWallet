package com.osscabinet.ossWallet.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(
        @Path("base") base: String
    ): ExchangeRateApiResponse

    @GET("v6/codes")
    suspend fun getSupportedCodes(): ExchangeRateCodesResponse
}

@Serializable
data class ExchangeRateCodesResponse(
    @SerialName("result") val result: String,
    @SerialName("supported_codes") val supportedCodes: List<List<String>>
)

@Serializable
data class ExchangeRateApiResponse(
    @SerialName("result") val result: String,
    @SerialName("base_code") val baseCode: String,
    @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long? = null,
    @SerialName("time_next_update_unix") val timeNextUpdateUnix: Long? = null,
    @SerialName("rates") val rates: Map<String, Double>
)
