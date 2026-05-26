package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.model.SystemCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override val categories: Flow<List<Category>> =
        categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertCategory(category: Category) =
        categoryDao.insertCategory(category.toEntity())

    override suspend fun deleteCategory(category: Category) {
        if (Category.isSystemCategory(category.id)) return
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun getAllCategoriesSync(): List<Category> =
        categoryDao.getAllCategoriesSync().map { it.toDomain() }
}
