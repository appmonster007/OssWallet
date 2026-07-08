package com.osscabinet.ossWallet.ui.screens.templates.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.dto.TemplatePrefillDTO
import com.osscabinet.ossWallet.domain.model.isOutside
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
import com.osscabinet.ossWallet.domain.usecase.EntryFormDataUseCase
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseEntryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

@HiltViewModel
class TemplateEntryViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val tagRepository: TagRepository,
    private val transactionUseCases: TransactionUseCases,
    entryFormDataUseCase: EntryFormDataUseCase,
    computationRepository: ComputationRepository,
    settingsRepository: SettingsRepository
) : BaseEntryViewModel(entryFormDataUseCase, computationRepository, settingsRepository) {

    private val _state = MutableStateFlow(TemplateEntryState())
    val state: StateFlow<TemplateEntryState> = _state.asStateFlow()

    private var lastInitParams: Pair<Long?, TemplatePrefillDTO?>? = null

    // BaseEntryViewModel abstract overrides
    override fun getSourceCurrency() = _state.value.source.currency
    override fun getTargetCurrency() = _state.value.destination.currency
    override fun getIsTransfer() = _state.value.isTransfer
    override fun getIsRateFixed() = _state.value.isRateFixed
    override fun getAmountValue() = _state.value.amountValue
    override fun applyRateUpdate(rate: BigDecimal, targetAmountStr: String) {
        _state.update {
            it.copy(
                exchangeRate = rate,
                destination = it.destination.copy(amount = targetAmountStr)
            )
        }
    }

    init {
        viewModelScope.launch {
            entryFormSnapshot.collect { snapshot ->
                _state.update { current ->
                    val defaultSource = if (!current.isEditMode &&
                        current.source.account is AccountRef.Unselected &&
                        snapshot.accounts.isNotEmpty()
                    ) {
                        current.source.copy(
                            account = AccountRef.fromStored(snapshot.accounts.first().id),
                            currency = snapshot.accounts.first().currencyCode
                        )
                    } else current.source
                    current.copy(
                        accounts = snapshot.accounts,
                        categories = snapshot.categories,
                        allTags = snapshot.allTags,
                        allPayees = snapshot.allPayees,
                        allNotes = snapshot.allNotes,
                        shortlistedCurrencies = snapshot.shortlistedCurrencies,
                        fullProviderCurrencies = snapshot.fullProviderCurrencies,
                        exchangeRates = snapshot.exchangeRates,
                        source = defaultSource,
                        selectedCategory = snapshot.categories.find { it.id == current.selectedCategoryId }
                    )
                }
            }
        }
    }

    fun initialize(templateId: Long? = null, prefill: TemplatePrefillDTO? = null) {
        if (lastInitParams == (templateId to prefill)) return
        if (lastInitParams?.second != null && prefill == null) return
        lastInitParams = templateId to prefill

        if (templateId == null) {
            if (prefill != null) {
                _state.update {
                    it.copy(
                        isEditMode = false,
                        templateId = null,
                        name = prefill.name,
                        source = TransactionSide(
                            account = if (prefill.entryMode == EntryMode.TRANSFER) AccountRef.fromTransferSide(
                                prefill.accountId
                            )
                            else AccountRef.fromStored(prefill.accountId),
                            amount = prefill.amount.abs().stripTrailingZeros().toPlainString(),
                            currency = prefill.currency ?: "USD",
                            categoryId = prefill.categoryId
                        ),
                        destination = TransactionSide(
                            amount = prefill.amount.abs().stripTrailingZeros().toPlainString()
                        ),
                        entryMode = prefill.entryMode,
                        note = prefill.note ?: "",
                        payee = prefill.payee ?: "",
                        location = prefill.location ?: "",
                        gpsLocation = prefill.gpsLocation,
                        warrantyExpirationMillis = prefill.warrantyExpiration
                            ?: (System.currentTimeMillis() + 31536000000L),
                        isWarrantyEnabled = prefill.warrantyExpiration != null,
                        transactionStatus = prefill.status,
                        paymentMethod = prefill.paymentMethod ?: PaymentMethod.CASH,
                        selectedTags = prefill.tags
                    )
                }
                updateCategoryName()
            } else {
                _state.update { current ->
                    val firstAccount = current.accounts.firstOrNull()
                    current.copy(
                        isEditMode = false,
                        templateId = null,
                        source = if (firstAccount != null && current.source.account is AccountRef.Unselected) {
                            current.source.copy(
                                account = AccountRef.fromStored(firstAccount.id),
                                currency = firstAccount.currencyCode
                            )
                        } else current.source
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            templateRepository.getTemplateById(templateId)?.let { template ->
                val isLinked = plannedPaymentRepository.isTemplateUsedInPlannedPayments(templateId)

                val sourceAccId = template.accountId
                val targetAccId = template.targetAccountId
                val sourceCurrency = template.currency
                val targetCurr = template.targetCurrency ?: ""
                val sourceAmtStr = template.amount.abs().stripTrailingZeros().toPlainString()
                val targetAmtStr = template.amount.abs().multiply(template.transferRate)
                    .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                    .toPlainString()
                var existingCategoryId = template.categoryId

                if (template.entryMode == EntryMode.TRANSFER) {
                    // Semantic Normalization: Unified category for both sides
                    existingCategoryId = if (Category.isInitialBalance(template.categoryId))
                        Category.SYSTEM_INITIAL_BALANCE else Category.SYSTEM_TRANSFER
                }

                val sourceRef = if (template.entryMode == EntryMode.TRANSFER)
                    AccountRef.fromTransferSide(sourceAccId) else AccountRef.fromStored(sourceAccId)
                val targetRef = if (template.entryMode == EntryMode.TRANSFER)
                    AccountRef.fromTransferSide(targetAccId) else AccountRef.fromStored(targetAccId)

                _state.update {
                    it.copy(
                        templateId = templateId,
                        isEditMode = true,
                        isLinkedToPlannedPayment = isLinked,
                        name = template.name,
                        source = TransactionSide(
                            account = sourceRef,
                            amount = sourceAmtStr,
                            currency = sourceCurrency,
                            categoryId = existingCategoryId
                        ),
                        destination = TransactionSide(
                            account = targetRef,
                            amount = targetAmtStr,
                            currency = targetCurr
                        ),
                        entryMode = template.entryMode,
                        exchangeRate = template.transferRate,
                        note = template.note ?: "",
                        payee = template.payee ?: "",
                        location = template.location ?: "",
                        gpsLocation = template.gpsLocation,
                        warrantyExpirationMillis = template.warrantyExpiration
                            ?: (System.currentTimeMillis() + 31536000000L),
                        isWarrantyEnabled = template.warrantyExpiration != null,
                        transactionStatus = template.transactionStatus,
                        paymentMethod = template.paymentMethod ?: PaymentMethod.CASH,
                        selectedTags = template.tags
                    )
                }
                updateCategoryName()
            }
        }
    }

    fun onEvent(event: TemplateEntryEvent) {
        when (event) {
            is TemplateEntryEvent.SetName -> _state.update { state ->
                val newNote = when {
                    state.note.isEmpty() -> "${event.name} - "
                    state.name.isNotEmpty() && state.note.startsWith("${state.name} - ") -> {
                        state.note.replaceFirst("${state.name} - ", "${event.name} - ")
                    }

                    else -> state.note
                }
                state.copy(name = event.name, note = newNote)
            }

            is TemplateEntryEvent.SetAmount -> {
                _state.update { s ->
                    s.applyUpdate(
                        TransactionEntryService.handleAmountChange(
                            amountStr = event.amount,
                            currentState = s.toUpdateResult(),
                            exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates)
                        )
                    )
                }
            }

            is TemplateEntryEvent.SetTargetAmount -> {
                _state.update { s ->
                    s.applyUpdate(
                        TransactionEntryService.handleTargetAmountChange(
                            amountStr = event.amount,
                            currentState = s.toUpdateResult(),
                            exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates)
                        )
                    )
                }
            }

            is TemplateEntryEvent.SetTransactionType -> {
                val oldMode = _state.value.entryMode
                val newMode = event.type
                _state.update { state ->
                    val result = TransactionEntryService.transitionType(
                        oldMode = oldMode,
                        newMode = newMode,
                        currentSource = state.source,
                        currentDestination = state.destination,
                        isSplitMode = false // Templates don't support split mode
                    )

                    state.copy(
                        entryMode = result.newMode,
                        source = result.source,
                        destination = result.destination
                    )
                }
                if (newMode == EntryMode.TRANSFER) {
                    recalculateExchangeRate()
                }
            }

            is TemplateEntryEvent.SetCategoryId -> {
                if (!Category.isInitialBalance(event.id)) {
                    _state.update { it.copy(source = it.source.copy(categoryId = event.id)) }
                    updateCategoryName()
                }
            }

            is TemplateEntryEvent.SetAccountId -> {
                _state.update { s ->
                    val result = TransactionEntryService.handleAccountSelection(
                        accountId = event.id,
                        isSource = true,
                        currentState = s.toUpdateResult(),
                        accounts = s.accounts,
                        baseCurrency = s.accounts.firstOrNull()?.currencyCode ?: "USD"
                    )
                    s.applyUpdate(result)
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TemplateEntryEvent.SetTargetAccountId -> {
                _state.update { s ->
                    val result = TransactionEntryService.handleAccountSelection(
                        accountId = event.id,
                        isSource = false,
                        currentState = s.toUpdateResult(),
                        accounts = s.accounts,
                        baseCurrency = s.accounts.firstOrNull()?.currencyCode ?: "USD"
                    )
                    s.applyUpdate(result)
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TemplateEntryEvent.SetCurrency -> {
                _state.update { state ->
                    val nextTarget = if (state.isTransfer && state.destination.account.isOutside) {
                        event.currency
                    } else state.targetCurrency

                    state.copy(
                        source = state.source.copy(currency = event.currency),
                        destination = state.destination.copy(currency = nextTarget)
                    )
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TemplateEntryEvent.SetTargetCurrency -> {
                _state.update { state ->
                    val nextSource = if (state.isTransfer && state.source.account.isOutside) {
                        event.currency
                    } else state.currency

                    state.copy(
                        destination = state.destination.copy(currency = event.currency),
                        source = state.source.copy(currency = nextSource)
                    )
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TemplateEntryEvent.SetExchangeRate -> _state.update {
                val sync = TransactionEntryService.syncTransferState(
                    sourceAmount = it.source.amountValue,
                    targetAmount = TransactionEntryService.calculateTargetAmountValue(
                        it.source.amountValue,
                        event.rate
                    ),
                    sourceCurrency = it.currency,
                    targetCurrency = it.targetCurrency,
                    isFixed = true,
                    activeSide = AmountEditingSide.SOURCE,
                    exchangeRates = ExchangeRateSnapshot(it.currency, it.exchangeRates)
                )

                it.copy(
                    isRateFixed = true,
                    exchangeRate = event.rate,
                    destination = it.destination.copy(
                        amount = sync.targetAmount.stripTrailingZeros().toPlainString()
                    )
                )
            }

            is TemplateEntryEvent.SetNote -> _state.update { it.copy(note = event.note) }
            is TemplateEntryEvent.SetPayee -> _state.update { it.copy(payee = event.payee) }
            is TemplateEntryEvent.SetLocation -> _state.update {
                it.copy(
                    gpsLocation = event.gpsLocation,
                    location = event.location ?: ""
                )
            }

            is TemplateEntryEvent.SetWarrantyEnabled -> _state.update { it.copy(isWarrantyEnabled = event.enabled) }
            is TemplateEntryEvent.SetWarrantyExpiration -> _state.update {
                it.copy(warrantyExpirationMillis = event.millis, showWarrantyDatePicker = false)
            }

            is TemplateEntryEvent.SetTransactionStatus -> _state.update { it.copy(transactionStatus = event.status) }
            is TemplateEntryEvent.SetPaymentMethod -> _state.update { it.copy(paymentMethod = event.method) }
            is TemplateEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is TemplateEntryEvent.SetLinkedTransactionIds -> {
                viewModelScope.launch {
                    withProfile { profileId ->
                        val transactions = event.ids.mapNotNull { id ->
                            transactionUseCases.getTransactionById(id, profileId)
                        }
                        _state.update {
                            it.copy(
                                displayLinkedTxs = transactions,
                                linkedTransactionIds = event.ids
                            )
                        }
                    }
                }
            }

            is TemplateEntryEvent.RemoveLinkedTransaction -> {
                _state.update { state ->
                    val newList = state.displayLinkedTxs.filter { it.id != event.targetId }
                    state.copy(
                        displayLinkedTxs = newList,
                        linkedTransactionIds = state.linkedTransactionIds.filter { it != event.targetId }
                    )
                }
            }

            is TemplateEntryEvent.AddTag -> {
                viewModelScope.launch {
                    tagRepository.insertTag(Tag(name = event.name, colorHex = event.color ?: -1))
                }
            }

            is TemplateEntryEvent.UpdateTag -> {
                viewModelScope.launch {
                    tagRepository.updateTag(event.tag.name, event.tag)
                }
            }

            is TemplateEntryEvent.DeleteTag -> {
                viewModelScope.launch {
                    tagRepository.deleteTag(event.tag)
                }
            }

            is TemplateEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is TemplateEntryEvent.ShowTargetAccountSheet -> _state.update {
                it.copy(
                    showTargetAccountSheet = event.show
                )
            }

            is TemplateEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is TemplateEntryEvent.ShowPaymentMethodSheet -> _state.update {
                it.copy(
                    showPaymentMethodSheet = event.show
                )
            }

            is TemplateEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is TemplateEntryEvent.ShowCurrencySheet -> _state.update { it.copy(showCurrencySheet = event.show) }
            is TemplateEntryEvent.ShowTargetCurrencySheet -> _state.update {
                it.copy(
                    showTargetCurrencySheet = event.show
                )
            }

            is TemplateEntryEvent.ShowExchangeRateDialog -> _state.update {
                it.copy(
                    showExchangeRateDialog = event.show
                )
            }

            is TemplateEntryEvent.ShowWarrantyDatePicker -> _state.update {
                it.copy(
                    showWarrantyDatePicker = event.show
                )
            }

            is TemplateEntryEvent.ShowLocationSheet -> _state.update { it.copy(showLocationSheet = event.show) }
            is TemplateEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is TemplateEntryEvent.ShowLinkedTransactionSheet -> _state.update {
                it.copy(
                    showLinkedTransactionSheet = event.show
                )
            }

            is TemplateEntryEvent.ShowNumpad -> _state.update {
                it.copy(
                    isNumpadVisible = event.show,
                    activeAmountSide = event.side
                )
            }

            is TemplateEntryEvent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = event.show) }

            is TemplateEntryEvent.SwapAccounts -> {
                _state.update { state ->
                    if (!state.isTransfer) return@update state
                    val (newSrc, newDst, newRate) = TransactionEntryService.swapAccounts(
                        source = state.source,
                        destination = state.destination,
                        transferRate = state.exchangeRate
                    )
                    state.copy(
                        source = newSrc,
                        destination = newDst,
                        exchangeRate = newRate
                    )
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TemplateEntryEvent.ToggleRateMode -> {
                _state.update { s ->
                    val nextFixed = !s.isRateFixed
                    s.copy(isRateFixed = nextFixed)
                }
                if (!_state.value.isRateFixed) {
                    recalculateExchangeRate()
                }
            }

            is TemplateEntryEvent.DismissAllSheets -> _state.update {
                it.copy(
                    showAccountSheet = false,
                    showTargetAccountSheet = false,
                    showCategorySheet = false,
                    showPaymentMethodSheet = false,
                    showStatusSheet = false,
                    showTagSheet = false,
                    showCurrencySheet = false,
                    showTargetCurrencySheet = false,
                    showExchangeRateDialog = false,
                    showWarrantyDatePicker = false,
                    showLocationSheet = false,
                    showLinkedTransactionSheet = false,
                )
            }

            is TemplateEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    private fun updateCategoryName() {
        val categoryId = _state.value.selectedCategoryId
        val category = _state.value.categories.find { it.id == categoryId }
        _state.update {
            it.copy(
                categoryName = category?.name ?: "Unknown",
                selectedCategory = category
            )
        }
    }

    fun saveTemplate(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val baseCurrency = settingsRepository.currency.first()

            val templateCategoryId = if (s.isTransfer) {
                if (Category.isInitialBalance(s.selectedCategoryId)) Category.SYSTEM_INITIAL_BALANCE
                else Category.SYSTEM_TRANSFER
            } else s.selectedCategoryId

            val request = TransactionEntryService.createRequest(
                name = s.name,
                amount = s.amountValue,
                entryMode = s.entryMode,
                sourceAccount = s.source.account,
                sourceCurrency = s.currency,
                targetAccount = s.destination.account,
                targetCurrency = s.targetCurrency,
                categoryId = templateCategoryId,
                note = s.note,
                payee = s.payee,
                tags = s.selectedTags,
                status = s.transactionStatus,
                targetAmountOverride = if (s.isTransfer) s.destination.amountValue else null,
                exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates),
                accounts = s.accounts,
                baseCurrency = baseCurrency
            )

            val template = Template(
                id = s.templateId ?: 0,
                name = request.name,
                categoryId = request.categoryId,
                amount = request.amount,
                entryMode = request.entryMode,
                accountId = request.accountId,
                targetAccountId = request.targetAccountId,
                targetCurrency = request.targetCurrency,
                transferRate = request.transferRate,
                note = request.note.ifBlank { null },
                payee = request.payee.ifBlank { null },
                location = s.location.ifBlank { null },
                gpsLocation = s.gpsLocation,
                warrantyExpiration = if (s.isWarrantyEnabled) s.warrantyExpirationMillis else null,
                transactionStatus = request.status,
                paymentMethod = s.paymentMethod,
                currency = request.currency,
                tags = request.tags,
                profileId = profileId
            )

            if (s.isEditMode) {
                templateRepository.updateTemplate(template)
            } else {
                templateRepository.insertTemplate(template)
            }
            onSuccess()
        }
    }

    fun deleteTemplate() {
        val id = _state.value.templateId ?: return
        viewModelScope.launch {
            templateRepository.getTemplateById(id)?.let {
                templateRepository.deleteTemplate(it)
            }
            _state.update { it.copy(navTriggered = true) }
        }
    }

    /**
     * Search transactions for linking.
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
        val profileId = settingsRepository.currentProfileId.first()
        emitAll(transactionUseCases.searchTransactions(profileId, query))
    }
}
