package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE profileId = :profileId ORDER BY name COLLATE NOCASE ASC")
    fun getAllTags(profileId: Int): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT id FROM tags WHERE name = :name COLLATE NOCASE AND profileId = :profileId LIMIT 1")
    suspend fun getTagIdByName(name: String, profileId: Int): Long?

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT * FROM tags WHERE name IN (:names) AND profileId = :profileId")
    suspend fun getTagsByNamesSync(names: List<String>, profileId: Int): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name IN (:names) AND profileId = :profileId")
    fun getTagsByNames(names: List<String>, profileId: Int): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsSync(): List<TagEntity>

    @Query("DELETE FROM tags")
    suspend fun clearAll()
}
