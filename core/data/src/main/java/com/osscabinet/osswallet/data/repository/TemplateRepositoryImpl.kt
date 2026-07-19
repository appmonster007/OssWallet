package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.TemplateDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao
) : TemplateRepository {
    override fun getAllTemplates(profileId: Int): Flow<List<Template>> =
        templateDao.getAllTemplates(profileId).map { list -> list.map { it.toDomain() } }

    override fun getStarredTemplates(): Flow<List<Template>> =
        templateDao.getStarredTemplates().map { list -> list.map { it.toDomain() } }

    override fun getStarredTemplates(profileId: Int): Flow<List<Template>> =
        templateDao.getStarredTemplates(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun updateStarStatus(id: Long, isStarred: Boolean) =
        templateDao.updateStarStatus(id, isStarred)

    override suspend fun insertTemplate(template: Template): Long =
        templateDao.insertTemplate(template.toEntity())

    override suspend fun updateTemplate(template: Template) =
        templateDao.updateTemplate(template.toEntity())

    override suspend fun deleteTemplate(template: Template) =
        templateDao.deleteTemplate(template.toEntity())

    override suspend fun getTemplateByName(name: String): Template? =
        templateDao.getTemplateByName(name)?.toDomain()

    override suspend fun getTemplateById(id: Long): Template? =
        templateDao.getTemplateById(id)?.toDomain()

    override suspend fun updateTemplates(templates: List<Template>) =
        templateDao.updateTemplates(templates.map { it.toEntity() })

    override suspend fun getAllTemplatesSync(profileId: Int): List<Template> =
        templateDao.getAllTemplatesSync(profileId).map { it.toDomain() }

    override suspend fun clearAll() = templateDao.clearAll()
}
