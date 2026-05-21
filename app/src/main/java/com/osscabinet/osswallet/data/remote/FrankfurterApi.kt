package com.osscabinet.ossWallet.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface FrankfurterApi {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") base: String
    ): FrankfurterResponse

    @GET("currencies")
    suspend fun getSupportedCurrencies(): Map<String, String>
}

@JsonClass(generateAdapter = true)
data class FrankfurterResponse(
    @Json(name = "amount") val amount: Double,
    @Json(name = "base") val base: String,
    @Json(name = "date") val date: String,
    @Json(name = "rates") val rates: Map<String, Double>
)
