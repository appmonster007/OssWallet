package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class EntryFormDataUseCase(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val templateRepository: TemplateRepository,
    private val transactionRepository: TransactionRepository
) {
    data class EntryCoreData(
        val accounts: List<Account>,
        val categories: List<Category>,
        val tags: List<Tag>,
        val templates: List<Template>,
        val payees: List<String>,
        val notes: List<String>
    )

    fun observeEntryCoreData(profileId: Int): Flow<EntryCoreData> {
        return combine(
            combine(
                accountRepository.getAllAccounts(profileId),
                categoryRepository.getAllCategories(profileId),
                tagRepository.getAllTags(profileId),
                templateRepository.getAllTemplates(profileId)
            ) { accounts, categories, tags, templates ->
                accounts to Triple(categories, tags, templates)
            },
            transactionRepository.observeUniquePayees(profileId),
            transactionRepository.observeUniqueNotes(profileId)
        ) { (accounts, triple), payees, notes ->
            val (categories, tags, templates) = triple
            EntryCoreData(accounts, categories, tags, templates, payees, notes)
        }
    }
}
