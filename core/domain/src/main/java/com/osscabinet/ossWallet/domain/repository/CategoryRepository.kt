package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    val categories: Flow<List<Category>>
    fun getAllCategories(profileId: Int): Flow<List<Category>>
    suspend fun upsertCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun getAllCategoriesSync(): List<Category>
}
