package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.osscabinet.ossWallet.data.local.entity.CardCacheEntity

@Dao
interface CardCacheDao {
    @Query("SELECT * FROM card_cache WHERE profileId = :profileId AND currency = :currency")
    suspend fun getCacheForProfile(profileId: Int, currency: String): List<CardCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CardCacheEntity)

    @Query("DELETE FROM card_cache WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: Int)

    @Query("DELETE FROM card_cache WHERE timestamp < :threshold")
    suspend fun pruneOldCache(threshold: Long)

    @Query("DELETE FROM card_cache WHERE profileId = :profileId AND currency = :currency AND cardId = :cardId")
    suspend fun deleteByCardId(profileId: Int, currency: String, cardId: String)
}
