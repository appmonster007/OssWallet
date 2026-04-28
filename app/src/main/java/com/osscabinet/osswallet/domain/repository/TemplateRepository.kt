package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Template
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<Template>>
    fun getStarredTemplates(): Flow<List<Template>>
    suspend fun updateStarStatus(id: Long, isStarred: Boolean)
    suspend fun insertTemplate(template: Template)
    suspend fun updateTemplate(template: Template)
    suspend fun deleteTemplate(template: Template)
    suspend fun getTemplateByName(name: String): Template?
    suspend fun updateTemplates(templates: List<Template>)
    suspend fun getAllTemplatesSync(): List<Template>
    suspend fun clearAll()
}
