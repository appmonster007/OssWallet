package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currency_rates")
    suspend fun getAllRates(): List<CurrencyRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<CurrencyRateEntity>)

    @Query("DELETE FROM currency_rates")
    suspend fun deleteAllRates()

    @Transaction
    suspend fun replaceRates(rates: List<CurrencyRateEntity>) {
        deleteAllRates()
        insertRates(rates)
    }
}
