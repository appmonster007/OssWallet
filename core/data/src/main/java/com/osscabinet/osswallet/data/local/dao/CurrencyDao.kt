package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    /**
     * Returns ALL cached rate rows across every base currency. Use [observeRatesForBase]
     * when you only need one base's rates (ARC-09 anchor correctness).
     */
    @Query("SELECT * FROM currency_rates")
    suspend fun getAllRates(): List<CurrencyRateEntity>

    @Query("SELECT * FROM currency_rates WHERE baseCurrency = :baseCurrency")
    suspend fun getRatesForBase(baseCurrency: String): List<CurrencyRateEntity>

    @Query("SELECT * FROM currency_rates WHERE baseCurrency = :baseCurrency")
    fun observeRatesForBase(baseCurrency: String): Flow<List<CurrencyRateEntity>>

    @Query("SELECT DISTINCT baseCurrency FROM currency_rates")
    suspend fun getCachedBaseCurrencies(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<CurrencyRateEntity>)

    /**
     * Whole-table wipe. ONLY for backup restore / dev reset. Profile-scoped or
     * base-scoped UI must use [deleteRatesForBase] (ARC-09).
     */
    @Query("DELETE FROM currency_rates")
    suspend fun clearAll()

    /**
     * ARC-09: delete ONLY rows belonging to [baseCurrency]. Whole-table wipe was
     * a race vector across concurrent fetches for different bases.
     */
    @Query("DELETE FROM currency_rates WHERE baseCurrency = :baseCurrency")
    suspend fun deleteRatesForBase(baseCurrency: String)

    @Transaction
    suspend fun replaceRatesForBase(baseCurrency: String, rates: List<CurrencyRateEntity>) {
        deleteRatesForBase(baseCurrency)
        insertRates(rates)
    }
}
