package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TemplateViewModel(
    private val templateRepository: TemplateRepository
) : ViewModel() {

    val templates: StateFlow<List<Template>> = templateRepository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starredTemplates: StateFlow<List<Template>> = templateRepository.getStarredTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleStar(template: Template) {
        viewModelScope.launch {
            templateRepository.updateStarStatus(template.id, !template.isStarred)
        }
    }

    fun addTemplate(
        name: String,
        categoryId: Long,
        amount: BigDecimal,
        transactionType: Int,
        icon: String? = null,
        colorHex: String? = null,
        accountId: String? = null,
        note: String? = null,
        payee: String? = null,
        location: String? = null,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            templateRepository.insertTemplate(
                Template(
                    id = 0,
                    name = name,
                    categoryId = categoryId,
                    amount = amount,
                    transactionType = transactionType,
                    icon = icon,
                    colorHex = colorHex,
                    accountId = accountId,
                    note = note,
                    payee = payee,
                    location = location,
                    paymentMethod = null,
                    currency = null,
                    tags = tags,
                    isStarred = false,
                    displayOrder = templates.value.size
                )
            )
        }
    }

    fun updateTemplate(template: Template) {
        viewModelScope.launch {
            templateRepository.updateTemplate(template)
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(template)
        }
    }

    fun updateTemplatesOrder(updatedTemplates: List<Template>) {
        viewModelScope.launch {
            val templatesWithOrder = updatedTemplates.mapIndexed { index, template ->
                template.copy(displayOrder = index)
            }
            templateRepository.updateTemplates(templatesWithOrder)
        }
    }
}
