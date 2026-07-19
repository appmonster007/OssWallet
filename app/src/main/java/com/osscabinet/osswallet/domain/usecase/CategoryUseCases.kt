package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class CategoryUseCases(
    private val categoryRepository: CategoryRepository
) {
    fun getCategories(profileId: Int): Flow<List<Category>> = categoryRepository.getAllCategories(profileId)

    suspend fun upsertCategory(
        id: Long = 0,
        name: String,
        iconName: String,
        colorHex: Int,
        parentId: Long? = null,
        isDefault: Boolean = false,
        nature: CategoryNature = CategoryNature.WANT
    ) {
        categoryRepository.upsertCategory(
            Category(
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

    suspend fun deleteCategory(category: Category) {
        categoryRepository.deleteCategory(category)
    }
}
