package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates WHERE profileId = :profileId ORDER BY name COLLATE NOCASE ASC")
    fun getAllTemplates(profileId: Int): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE isStarred = 1 ORDER BY displayOrder ASC")
    fun getStarredTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE isStarred = 1 AND profileId = :profileId ORDER BY displayOrder ASC")
    fun getStarredTemplates(profileId: Int): Flow<List<TemplateEntity>>

    @Query("UPDATE templates SET isStarred = :isStarred WHERE id = :templateId")
    suspend fun updateStarStatus(templateId: Long, isStarred: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: TemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)

    @Query("SELECT * FROM templates WHERE name = :name LIMIT 1")
    suspend fun getTemplateByName(name: String): TemplateEntity?

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): TemplateEntity?

    @Update
    suspend fun updateTemplates(templates: List<TemplateEntity>)

    @Query("SELECT * FROM templates WHERE profileId = :profileId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllTemplatesSync(profileId: Int): List<TemplateEntity>

    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllTemplatesSync(): List<TemplateEntity>

    @Query("DELETE FROM templates")
    suspend fun clearAll()
}
