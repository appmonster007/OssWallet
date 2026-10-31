package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override val categories: Flow<List<Category>> =
        categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override fun getAllCategories(profileId: Int): Flow<List<Category>> =
        categoryDao.getAllCategories(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertCategory(category: Category) {
        if (Category.isSystemCategory(category.id)) return
        val toInsert = if (category.id == 0L) {
            // A genuinely new category, not an edit of an existing one (editing always
            // passes a real, already-assigned id). Assign an explicit id at or above the
            // user-category floor instead of trusting Room's bare autoincrement, so this
            // category's id can never fall inside the range reserved for defaults -- even
            // if a future release adds more default categories above today's highest id.
            val nextId = maxOf(categoryDao.getMaxCategoryId() ?: 0L, Category.USER_CATEGORY_ID_FLOOR - 1) + 1
            category.copy(id = nextId)
        } else {
            category
        }
        categoryDao.insertCategory(toInsert.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        if (Category.isSystemCategory(category.id)) return
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun getAllCategoriesSync(): List<Category> =
        categoryDao.getAllCategoriesSync().map { it.toDomain() }
}
