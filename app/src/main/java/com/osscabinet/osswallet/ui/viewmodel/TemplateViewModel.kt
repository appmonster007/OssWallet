package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.dao.TemplateDao
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import java.math.BigDecimal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TemplateViewModel(private val templateDao: TemplateDao) : ViewModel() {

    val templates: StateFlow<List<TemplateEntity>> = templateDao.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val starredTemplates: StateFlow<List<TemplateEntity>> = templateDao.getStarredTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        seedTemplates()
    }

    private fun seedTemplates() {
        viewModelScope.launch {
            val list = templateDao.getAllTemplates().first()
            if (list.isEmpty()) {
                val initialTemplates = listOf(
                    TemplateEntity(name = "Grocery Run", category = "Food", amount = BigDecimal.ZERO, transactionType = 0, isStarred = true, accountId = "default-cash-id", displayOrder = 0),
                    TemplateEntity(name = "Salary Deposit", category = "Salary", amount = BigDecimal.ZERO, transactionType = 1, isStarred = true, accountId = "default-cash-id", displayOrder = 1),
                    TemplateEntity(name = "Rent Payment", category = "Rent", amount = BigDecimal.ZERO, transactionType = 0, isStarred = true, accountId = "default-cash-id", displayOrder = 2),
                    TemplateEntity(name = "Coffee", category = "Food", amount = BigDecimal.ZERO, transactionType = 0, isStarred = true, accountId = "default-cash-id", displayOrder = 3)
                )
                initialTemplates.forEach { templateDao.insertTemplate(it) }
            }
        }
    }

    fun toggleStar(template: TemplateEntity) {
        viewModelScope.launch {
            templateDao.updateStarStatus(template.id, !template.isStarred)
        }
    }

    fun addTemplate(
        name: String,
        category: String,
        amount: BigDecimal,
        type: Int,
        note: String? = null,
        accountId: String? = null,
        payee: String? = null,
        location: String? = null,
        paymentMethod: String? = null,
        currency: String? = null,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            templateDao.insertTemplate(
                TemplateEntity(
                    name = name,
                    category = category,
                    amount = amount,
                    transactionType = type,
                    note = note,
                    accountId = accountId,
                    payee = payee,
                    location = location,
                    paymentMethod = paymentMethod,
                    currency = currency,
                    tags = tags.joinToString(",")
                )
            )
        }
    }

    fun updateTemplate(template: TemplateEntity) {
        viewModelScope.launch {
            templateDao.updateTemplate(template)
        }
    }

    fun deleteTemplate(template: TemplateEntity) {
        viewModelScope.launch {
            templateDao.deleteTemplate(template)
        }
    }

    fun updateTemplatesOrder(orderedTemplates: List<TemplateEntity>) {
        viewModelScope.launch {
            val entities = orderedTemplates.mapIndexed { index, template ->
                template.copy(displayOrder = index)
            }
            templateDao.updateTemplates(entities)
        }
    }
}
