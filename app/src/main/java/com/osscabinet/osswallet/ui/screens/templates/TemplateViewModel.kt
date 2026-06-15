package com.osscabinet.ossWallet.ui.screens.templates

import com.osscabinet.ossWallet.domain.model.EntryMode
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    settingsRepository: SettingsRepository,
) : BaseViewModel(settingsRepository) {

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
        entryMode: EntryMode,
        icon: String? = null,
        colorHex: String? = null,
        accountId: String? = null,
        targetAccountId: String? = null,
        targetCurrency: String? = null,
        transferRate: BigDecimal = BigDecimal.ONE,
        note: String? = null,
        payee: String? = null,
        location: String? = null,
        paymentMethod: PaymentMethod? = null,
        currency: String,
        tags: Set<String> = emptySet(),
        profileId: Int = 1
    ) {
        viewModelScope.launch {
            templateRepository.insertTemplate(
                Template(
                    id = 0,
                    name = name,
                    categoryId = categoryId,
                    amount = amount,
                    entryMode = entryMode,
                    icon = icon,
                    colorHex = colorHex,
                    accountId = accountId,
                    targetAccountId = targetAccountId,
                    targetCurrency = targetCurrency,
                    transferRate = transferRate,
                    note = note,
                    payee = payee,
                    location = location,
                    paymentMethod = paymentMethod,
                    currency = currency,
                    tags = tags,
                    isStarred = false,
                    displayOrder = templates.value.size,
                    profileId = profileId
                )
            )
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
