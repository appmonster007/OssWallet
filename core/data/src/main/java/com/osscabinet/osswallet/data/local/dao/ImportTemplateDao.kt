package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.osscabinet.ossWallet.data.local.entity.ImportTemplateEntity

@Dao
interface ImportTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: ImportTemplateEntity)

    @Query("SELECT * FROM import_templates WHERE sourceApp = :sourceApp LIMIT 1")
    suspend fun getTemplate(sourceApp: String): ImportTemplateEntity?
}
