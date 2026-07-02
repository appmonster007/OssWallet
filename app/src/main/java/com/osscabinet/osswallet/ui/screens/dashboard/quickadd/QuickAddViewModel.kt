package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.toDbTransferSide
import com.osscabinet.ossWallet.domain.model.isOutside
import com.osscabinet.ossWallet.domain.model.resolve
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface QuickAddResult {
    data class TemplateItem(
        val template: Template,
        val iconName: String?,
        val colorHex: Int
    ) : QuickAddResult
    data class CategoryItem(val category: Category) : QuickAddResult
    data class SuggestionItem(
        val label: String,
        val categoryId: Long,
        val iconName: String?,
        val colorHex: Int,
        val accountId: String?,
        val targetAccountId: String? = null,
        val amount: BigDecimal,
        val currency: String,
        val targetCurrency: String? = null,
        val entryMode: EntryMode,
        val note: String,
        val payee: String,
        val tags: Set<String>
    ) : QuickAddResult
}

data class QuickAddSection(
    val title: String,
    val items: List<QuickAddResult>
)

data class QuickAddState(
    val currentStep: Int = 0,
    val searchQuery: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val selectedTemplate: Template? = null,
    val selectedCategoryId: Long? = Category.SYSTEM_UNKNOWN,
    val selectedAccount: AccountRef = AccountRef.Unselected,
    val selectedToAccount: AccountRef = AccountRef.Unselected,
    val selectedCurrency: String = "",
    val selectedToCurrency: String = "",
    val amountOverride: BigDecimal? = null,
    val isAmountOverridden: Boolean = false,
    val activeAmountSide: AmountEditingSide = AmountEditingSide.SOURCE,
    val entryMode: EntryMode = EntryMode.EXPENSE,
    val note: String = "",
    val payee: String = "",
    val tags: Set<String> = emptySet(),
    val accounts: List<Account> = emptyList(),
    val structureTypes: List<AccountStructureType> = emptyList(),
    val accountBalances: Map<String, BigDecimal> = emptyMap(),
    val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot.EMPTY,
    val baseCurrency: String = "USD",
    val isSaving: Boolean = false
) {
    val selectedAccountId: String? get() = (selectedAccount as? AccountRef.Wallet)?.id ?: if (selectedAccount.isOutside) "" else null
    val selectedToAccountId: String? get() = (selectedToAccount as? AccountRef.Wallet)?.id ?: if (selectedToAccount.isOutside) "" else null

    val effectiveTargetAmount: BigDecimal
        get() = amountOverride ?: if (entryMode == EntryMode.TRANSFER) {
            val rate = TransactionEntryService.getMarketRate(selectedCurrency, selectedToCurrency, exchangeRates)
            TransactionEntryService.calculateTargetAmountValue(amount, rate)
        } else BigDecimal.ZERO
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val transactionManager: TransactionManager,
    private val transactionRepository: TransactionRepository,
    private val computationRepository: ComputationRepository,
    templateRepository: TemplateRepository,
    categoryRepository: CategoryRepository,
) : BaseViewModel(settingsRepository) {

    private val initialType: EntryMode = EntryMode.EXPENSE

    val templates: StateFlow<List<Template>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> templateRepository.getAllTemplates(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Template>())

    val categories: StateFlow<List<Category>> = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Category>())

    private val _uiState = MutableStateFlow(QuickAddState(entryMode = initialType))

    init {
        viewModelScope.launch {
            computationRepository.exchangeRatesSnapshot.collect { rates ->
                _uiState.update { it.copy(exchangeRates = rates) }
            }
        }
    }

    val sections: StateFlow<List<QuickAddSection>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                _uiState.map { it.searchQuery },
                templates,
                categories,
                transactionRepository.getAllTransactions(profileId)
            ) { query, allTemplates, allCategories, allTransactions ->
                val sections = mutableListOf<QuickAddSection>()

                // 1. Templates Section
                val matchedTemplates = allTemplates.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.note?.contains(query, ignoreCase = true) == true
                }.sortedByDescending { it.isStarred }
                
                if (matchedTemplates.isNotEmpty()) {
                    sections.add(
                        QuickAddSection(
                            title = "TEMPLATES",
                            items = matchedTemplates.map { t ->
                                val cat = allCategories.find { it.id == t.categoryId }
                                QuickAddResult.TemplateItem(
                                    template = t,
                                    iconName = t.icon ?: cat?.iconName,
                                    colorHex = t.colorHex?.let {
                                        runCatching { android.graphics.Color.parseColor(it) }.getOrNull()
                                    } ?: cat?.colorHex ?: -1
                                )
                            }
                        )
                    )
                }

                // 2. Suggestions Section (Only when searching)
                if (query.isNotBlank()) {
                    val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(367)
                    val suggestions = allTransactions
                        .filter { it.timestamp >= cutoff }
                        .filter {
                            it.note.contains(query, ignoreCase = true) ||
                                    it.payee.contains(query, ignoreCase = true)
                        }
                        .mapNotNull { tx ->
                            val label = when {
                                tx.note.isNotBlank() && tx.payee.isNotBlank() -> "${tx.note}: ${tx.payee}"
                                tx.note.isNotBlank() -> tx.note
                                tx.payee.isNotBlank() -> tx.payee
                                else -> null
                            }
                            label?.let { l -> tx to l }
                        }
                        .groupBy { it.second } // Group by label
                        .map { (label, pairs) ->
                            val latest = pairs.maxBy { it.first.timestamp }.first
                            val cat = allCategories.find { it.id == latest.categoryId }
                            
                            if (latest.transactionType.isTransfer) {
                                // Try to find counterpart
                                val counterpart = allTransactions.find { 
                                    it.id != latest.id && 
                                    it.transactionType.isTransfer && 
                                    it.transactionType != latest.transactionType &&
                                    Math.abs(it.timestamp - latest.timestamp) < 5000 &&
                                    it.amount.abs() == latest.amount.abs()
                                }
                                
                                // Source should be the one with OUTFLOW or negative amount
                                val sourceLeg = if (latest.transactionType == com.osscabinet.ossWallet.domain.model.TransactionType.TRANSFER_OUTFLOW) latest else counterpart
                                val targetLeg = if (latest.transactionType == com.osscabinet.ossWallet.domain.model.TransactionType.TRANSFER_INFLOW) latest else counterpart
                                
                                QuickAddResult.SuggestionItem(
                                    label = label,
                                    categoryId = latest.categoryId,
                                    iconName = cat?.iconName,
                                    colorHex = cat?.colorHex ?: -1,
                                    accountId = sourceLeg?.accountId,
                                    targetAccountId = targetLeg?.accountId,
                                    amount = sourceLeg?.amount?.abs() ?: latest.amount.abs(),
                                    currency = sourceLeg?.currency ?: latest.currency,
                                    targetCurrency = targetLeg?.currency,
                                    entryMode = EntryMode.TRANSFER,
                                    note = latest.note,
                                    payee = latest.payee,
                                    tags = latest.tags
                                )
                            } else {
                                QuickAddResult.SuggestionItem(
                                    label = label,
                                    categoryId = latest.categoryId,
                                    iconName = cat?.iconName,
                                    colorHex = cat?.colorHex ?: -1,
                                    accountId = latest.accountId,
                                    targetAccountId = null,
                                    amount = latest.amount.abs(),
                                    currency = latest.currency,
                                    targetCurrency = null,
                                    entryMode = if (latest.amount >= BigDecimal.ZERO) EntryMode.INCOME else EntryMode.EXPENSE,
                                    note = latest.note,
                                    payee = latest.payee,
                                    tags = latest.tags
                                )
                            }
                        }
                        .sortedByDescending { it.label } // Or some other relevance
                        .take(5)

                    if (suggestions.isNotEmpty()) {
                        sections.add(
                            QuickAddSection(
                                title = "SUGGESTIONS",
                                items = suggestions
                            )
                        )
                    }
                }

                // 3. Categories Section
                val matchedCategories = allCategories
                    .filter { !Category.isSystemCategory(it.id) }
                    .filter { it.name.contains(query, ignoreCase = true) }
                    .sortedBy { it.name }

                if (matchedCategories.isNotEmpty()) {
                    sections.add(
                        QuickAddSection(
                            title = "CATEGORIES",
                            items = matchedCategories.map { QuickAddResult.CategoryItem(it) }
                        )
                    )
                }

                sections
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<QuickAddState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                _uiState,
                accountRepository.getAllAccounts(profileId),
                transactionRepository.getAllTransactions(profileId),
                settingsRepository.currency,
                computationRepository.exchangeRatesSnapshot
            ) { uiState, allAccounts, transactions, baseCurrency, rates ->
                val baseAccounts = allAccounts.filter { !it.isArchived }
                val activeAccounts = if (uiState.entryMode == EntryMode.TRANSFER) {
                    listOf(Account.OUTSIDE_WALLET) + baseAccounts
                } else {
                    baseAccounts
                }
                
                // Sort accounts: preselected first (from template/suggestion), then by name
                val sortedAccounts = activeAccounts.sortedWith(
                    compareByDescending<Account> { AccountRef.fromStored(it.id) == uiState.selectedAccount }
                        .thenBy { it.name }
                )

                val structureTypes = baseAccounts.map { it.structureType }
                    .distinct()
                    .sortedBy { it.displayName }

                val balances = baseAccounts.associate { acc ->
                    acc.id to AggregationService.calculateAccountBalance(
                        acc.id,
                        transactions,
                        baseCurrency,
                        rates
                    )
                }

                uiState.copy(
                    accounts = sortedAccounts,
                    structureTypes = structureTypes,
                    accountBalances = balances,
                    exchangeRates = rates,
                    baseCurrency = baseCurrency
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            QuickAddState(entryMode = initialType)
        )

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTemplateSelected(template: Template) {
        val prefill = TransactionEntryService.resolvePrefill(
            entryMode = template.entryMode,
            accountId = template.accountId,
            targetAccountId = template.targetAccountId,
            currency = template.currency,
            targetCurrency = template.targetCurrency,
            amount = template.amount,
            categoryId = template.categoryId,
            note = template.note,
            payee = template.payee,
            tags = template.tags,
            accounts = state.value.accounts
        )
        _uiState.update {
            it.copy(
                selectedTemplate = template,
                selectedCategoryId = prefill.categoryId,
                selectedAccount = prefill.account,
                selectedToAccount = prefill.targetAccount,
                selectedCurrency = prefill.currency,
                selectedToCurrency = prefill.targetCurrency,
                entryMode = prefill.entryMode,
                amount = prefill.amount,
                note = prefill.note,
                payee = prefill.payee,
                tags = prefill.tags,
                currentStep = 1
            )
        }
    }

    fun onSuggestionSelected(
        label: String,
        categoryId: Long,
        accountId: String?,
        targetAccountId: String? = null,
        amount: BigDecimal,
        currency: String,
        targetCurrency: String? = null,
        entryMode: EntryMode,
        note: String,
        payee: String,
        tags: Set<String>
    ) {
        val prefill = TransactionEntryService.resolvePrefill(
            entryMode = entryMode,
            accountId = accountId,
            targetAccountId = targetAccountId,
            currency = currency,
            targetCurrency = targetCurrency,
            amount = amount,
            categoryId = categoryId,
            note = note,
            payee = payee,
            tags = tags,
            accounts = state.value.accounts
        )
        _uiState.update {
            it.copy(
                selectedTemplate = null,
                selectedCategoryId = prefill.categoryId,
                selectedAccount = prefill.account,
                selectedToAccount = prefill.targetAccount,
                selectedCurrency = prefill.currency,
                selectedToCurrency = prefill.targetCurrency,
                entryMode = prefill.entryMode,
                amount = prefill.amount,
                note = prefill.note,
                payee = prefill.payee,
                tags = prefill.tags,
                currentStep = 1
            )
        }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update {
            it.copy(
                selectedTemplate = null,
                selectedCategoryId = category.id,
                selectedAccount = AccountRef.Unselected,
                entryMode = EntryMode.EXPENSE,
                amount = BigDecimal.ZERO,
                note = "",
                payee = "",
                tags = emptySet(),
                currentStep = 1
            )
        }
    }

    fun onAmountChanged(amount: BigDecimal) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun onAmountOverrideChanged(amount: BigDecimal) {
        _uiState.update { it.copy(amountOverride = amount) }
    }

    fun setAmountFocus(side: AmountEditingSide) {
        _uiState.update { s ->
            // If switching to Source, we keep the existing override mode.
            // If switching to Target, we MUST be in manual mode to edit it.
            val isManual = if (side == AmountEditingSide.TARGET) true else s.isAmountOverridden
            
            s.copy(
                activeAmountSide = side,
                isAmountOverridden = isManual,
                amountOverride = if (isManual && s.amountOverride == null) {
                    val rate = TransactionEntryService.getMarketRate(s.selectedCurrency, s.selectedToCurrency, s.exchangeRates)
                    TransactionEntryService.calculateTargetAmountValue(s.amount, rate)
                } else s.amountOverride
            )
        }
    }

    fun toggleAmountOverride() {
        _uiState.update { s ->
            val next = !s.isAmountOverridden
            s.copy(
                isAmountOverridden = next,
                activeAmountSide = if (!next) AmountEditingSide.SOURCE else s.activeAmountSide,
                amountOverride = if (next && s.amountOverride == null) {
                    val rate = TransactionEntryService.getMarketRate(s.selectedCurrency, s.selectedToCurrency, s.exchangeRates)
                    if (rate.signum() != 0) {
                        TransactionEntryService.calculateTargetAmountValue(s.amount, rate)
                    } else BigDecimal.ZERO
                } else if (!next) null else s.amountOverride
            )
        }
    }

    fun onSwapAccounts() {
        _uiState.update { s ->
            if (s.entryMode != EntryMode.TRANSFER) return@update s
            val rates = s.exchangeRates
            val rate = TransactionEntryService.getMarketRate(s.selectedCurrency, s.selectedToCurrency, rates)
            
            val (newSrc, newDst, newRate) = TransactionEntryService.swapAccounts(
                source = TransactionSide(
                    account = s.selectedAccount,
                    amount = s.amount.stripTrailingZeros().toPlainString(),
                    currency = s.selectedCurrency
                ),
                destination = TransactionSide(
                    account = s.selectedToAccount,
                    amount = s.amountOverride?.stripTrailingZeros()?.toPlainString() ?: "",
                    currency = s.selectedToCurrency
                ),
                transferRate = rate
            )

            s.copy(
                selectedAccount = newSrc.account,
                selectedToAccount = newDst.account,
                selectedCurrency = newSrc.currency,
                selectedToCurrency = newDst.currency,
                amount = newSrc.amountValue,
                amountOverride = if (s.isAmountOverridden) newDst.amountValue else null
            )
        }
    }

    fun goToStep(step: Int) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun onAccountTapped(accountId: String, isFrom: Boolean = true) {
        val account = state.value.accounts.find { it.id == accountId }
        val newRef = if (accountId.isBlank()) AccountRef.Outside else AccountRef.Wallet(accountId)
        _uiState.update { s ->
            if (isFrom) {
                val newCurrency = TransactionEntryService.resolveSideCurrency(
                    account = newRef,
                    accountCurrency = account?.currencyCode,
                    currentCurrency = s.selectedCurrency,
                    baseCurrency = s.baseCurrency
                )
                s.copy(
                    selectedAccount = newRef,
                    selectedCurrency = newCurrency,
                    currentStep = if (s.entryMode == EntryMode.TRANSFER) s.currentStep else 2
                )
            } else {
                val newCurrency = TransactionEntryService.resolveSideCurrency(
                    account = newRef,
                    accountCurrency = account?.currencyCode,
                    currentCurrency = s.selectedCurrency, // QuickAdd inherits from Source by default
                    baseCurrency = s.baseCurrency
                )
                s.copy(
                    selectedToAccount = newRef,
                    selectedToCurrency = newCurrency,
                )
            }
        }
    }

    fun onCurrencySelected(currency: String, isFrom: Boolean = true) {
        _uiState.update {
            if (isFrom) it.copy(selectedCurrency = currency)
            else it.copy(selectedToCurrency = currency)
        }
    }

    fun toggleTransactionType() {
        _uiState.update { s ->
            val nextMode = when (s.entryMode) {
                EntryMode.EXPENSE -> EntryMode.INCOME
                EntryMode.INCOME -> EntryMode.TRANSFER
                EntryMode.TRANSFER -> EntryMode.EXPENSE
            }
            
            val result = TransactionEntryService.transitionType(
                oldMode = s.entryMode,
                newMode = nextMode,
                currentSource = TransactionSide(
                    account = s.selectedAccount,
                    amount = s.amount.stripTrailingZeros().toPlainString(),
                    currency = s.selectedCurrency,
                    categoryId = s.selectedCategoryId ?: Category.SYSTEM_UNKNOWN
                ),
                currentDestination = TransactionSide(
                    account = s.selectedToAccount,
                    amount = s.amountOverride?.stripTrailingZeros()?.toPlainString() ?: "",
                    currency = s.selectedToCurrency,
                    categoryId = Category.SYSTEM_TRANSFER
                ),
                isSplitMode = false
            )

            s.copy(
                entryMode = result.newMode,
                selectedAccount = result.source.account,
                selectedToAccount = result.destination.account,
                selectedCurrency = result.source.currency,
                selectedToCurrency = result.destination.currency,
                amount = result.source.amountValue,
                amountOverride = if (result.destination.amount.isBlank()) null else result.destination.amountValue,
                selectedCategoryId = result.source.categoryId,
                isAmountOverridden = false
            )
        }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val s = state.value
        if (!canSave) return
        val amount = s.amount
        if (amount < BigDecimal.ZERO) return
        
        val categoryId = s.selectedCategoryId ?: Category.SYSTEM_OTHERS
        val name = s.selectedTemplate?.name
            ?: categories.value.find { it.id == categoryId }?.name
            ?: if (s.entryMode == EntryMode.TRANSFER) "Transfer" else "Quick Transaction"

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                withProfile { profileId ->
                    val request = TransactionEntryService.createRequest(
                        name = name,
                        amount = amount,
                        entryMode = s.entryMode,
                        sourceAccount = s.selectedAccount,
                        sourceCurrency = s.selectedCurrency,
                        targetAccount = s.selectedToAccount,
                        targetCurrency = s.selectedToCurrency,
                        categoryId = categoryId,
                        note = s.note.ifBlank { s.selectedTemplate?.note ?: "" },
                        payee = s.payee.ifBlank { s.selectedTemplate?.payee ?: "" },
                        status = s.selectedTemplate?.transactionStatus ?: TransactionStatus.CLEARED,
                        tags = s.tags.ifEmpty { s.selectedTemplate?.tags ?: emptySet() },
                        amountOverride = s.amountOverride,
                        exchangeRates = s.exchangeRates,
                        accounts = s.accounts,
                        baseCurrency = s.baseCurrency
                    )
                    transactionManager.handleSaveTransaction(
                        existingTransaction = null,
                        request = request,
                        profileId = profileId
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun reset() {
        val currentAccounts = state.value.accounts
        _uiState.value = QuickAddState(accounts = currentAccounts, entryMode = initialType)
    }

    val canSave: Boolean
        get() {
            val s = state.value
            if (s.amount < BigDecimal.ZERO) return false
            
            return TransactionEntryService.validateAccountSelection(
                entryMode = s.entryMode,
                source = s.selectedAccount,
                target = s.selectedToAccount
            ).isValid
        }
}
