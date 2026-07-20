package com.osscabinet.ossWallet.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

@Serializable
data class FrankfurterResponse(
    @SerialName("amount") val amount: Double,
    @SerialName("base") val base: String,
    @SerialName("date") val date: String,
    @SerialName("rates") val rates: Map<String, Double>
)
