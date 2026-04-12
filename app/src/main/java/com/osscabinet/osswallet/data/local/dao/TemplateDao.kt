package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE ASC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE isStarred = 1 ORDER BY displayOrder ASC")
    fun getStarredTemplates(): Flow<List<TemplateEntity>>

    @Query("UPDATE templates SET isStarred = :isStarred WHERE id = :templateId")
    suspend fun updateStarStatus(templateId: Long, isStarred: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity)

    @Update
    suspend fun updateTemplate(template: TemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)

    @Query("SELECT * FROM templates WHERE name = :name LIMIT 1")
    suspend fun getTemplateByName(name: String): TemplateEntity?

    @Update
    suspend fun updateTemplates(templates: List<TemplateEntity>)

    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllTemplatesSync(): List<TemplateEntity>

    @Query("DELETE FROM templates")
    suspend fun clearAll()
}
