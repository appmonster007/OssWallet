package com.osscabinet.gleemanager.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApi {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(
        @Path("base") base: String
    ): CurrencyResponse
}

data class CurrencyResponse(
    val result: String,
    val provider: String,
    val documentation: String,
    val terms_of_use: String,
    val time_last_update_unix: Long,
    val time_last_update_utc: String,
    val time_next_update_unix: Long,
    val time_next_update_utc: String,
    val base_code: String,
    val rates: Map<String, Double>
)
