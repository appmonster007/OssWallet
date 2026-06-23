package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Template
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getAllTemplates(profileId: Int): Flow<List<Template>>

    /**
     * Intentionally cross-profile — reserved for future features that need a
     * global view of starred templates. DO NOT use from profile-scoped UI;
     * prefer [getStarredTemplates] with a profileId.
     */
    fun getStarredTemplates(): Flow<List<Template>>

    fun getStarredTemplates(profileId: Int): Flow<List<Template>>
    suspend fun updateStarStatus(id: Long, isStarred: Boolean)
    suspend fun insertTemplate(template: Template): Long
    suspend fun updateTemplate(template: Template)
    suspend fun deleteTemplate(template: Template)
    suspend fun getTemplateByName(name: String): Template?
    suspend fun getTemplateById(id: Long): Template?
    suspend fun updateTemplates(templates: List<Template>)
    suspend fun getAllTemplatesSync(profileId: Int): List<Template>
    suspend fun clearAll()
}
