package com.osscabinet.gleemanager.data.local.dao

import androidx.room.*
import com.osscabinet.gleemanager.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}
