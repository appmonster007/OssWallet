package com.osscabinet.ossWallet.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
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

@JsonClass(generateAdapter = true)
data class ExchangeRateCodesResponse(
    @Json(name = "result") val result: String,
    @Json(name = "supported_codes") val supportedCodes: List<List<String>>
)

@JsonClass(generateAdapter = true)
data class ExchangeRateApiResponse(
    @Json(name = "result") val result: String,
    @Json(name = "base_code") val baseCode: String,
    @Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long? = null,
    @Json(name = "time_next_update_unix") val timeNextUpdateUnix: Long? = null,
    @Json(name = "rates") val rates: Map<String, Double>
)
