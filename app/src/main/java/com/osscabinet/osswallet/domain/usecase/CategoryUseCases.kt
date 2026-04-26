package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryUseCases(
    private val categoryDao: CategoryDao
) {
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun upsertCategory(
        id: Long = 0,
        name: String,
        iconName: String,
        colorHex: Int,
        parentId: Long? = null,
        isDefault: Boolean = false
    ) {
        categoryDao.insertCategory(
            CategoryEntity(
                id = id,
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                parentId = parentId,
                isDefault = isDefault
            )
        )
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }
}
