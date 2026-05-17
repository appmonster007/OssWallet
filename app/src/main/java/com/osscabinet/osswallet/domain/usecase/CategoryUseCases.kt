package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.domain.model.CategoryNature
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
        isDefault: Boolean = false,
        nature: CategoryNature = CategoryNature.WANT
    ) {
        categoryDao.insertCategory(
            CategoryEntity(
                id = id,
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                parentId = parentId,
                isDefault = isDefault,
                nature = nature
            )
        )
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }
}
