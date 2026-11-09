package com.osscabinet.ossWallet.ui.screens.templates.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
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
import java.math.RoundingMode

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

    // region BaseEntryViewModel hooks

    override fun getSourceCurrency() = _state.value.source.currency
    override fun getTargetCurrency() = _state.value.destination.currency
    override fun getIsTransfer() = _state.value.isTransfer
    override fun getIsRateFixed() = _state.value.isRateFixed
    override fun getAmountValue() = _state.value.amountValue
    override fun applyRateUpdate(rate: BigDecimal, targetAmountStr: String) {
        _state.update { it.copy(exchangeRate = rate, destination = it.destination.copy(amount = targetAmountStr)) }
    }

    // endregion

    init {
        viewModelScope.launch {
            entryFormSnapshot.collect { snapshot ->
                _state.update { current ->
                    val firstAccount = snapshot.accounts.firstOrNull()
                    current.copy(
                        accounts = snapshot.accounts,
                        categories = snapshot.categories,
                        allTags = snapshot.allTags,
                        allPayees = snapshot.allPayees,
                        allNotes = snapshot.allNotes,
                        shortlistedCurrencies = snapshot.shortlistedCurrencies,
                        fullProviderCurrencies = snapshot.fullProviderCurrencies,
                        exchangeRates = snapshot.exchangeRates,
                        source = if (current.isEditMode) current.source else current.source.withDefaultAccount(firstAccount),
                        selectedCategory = snapshot.categories.find { it.id == current.selectedCategoryId }
                    )
                }
            }
        }
    }

    // region Initialisation

    fun initialize(templateId: Long? = null, prefill: TemplatePrefillDTO? = null) {
        val params = templateId to prefill
        if (lastInitParams == params) return
        if (lastInitParams?.second != null && prefill == null) return
        lastInitParams = params

        when {
            templateId != null -> loadTemplate(templateId)
            prefill != null -> {
                _state.update { it.withPrefill(prefill) }
                updateCategoryName()
            }
            else -> _state.update {
                it.copy(
                    isEditMode = false,
                    templateId = null,
                    source = it.source.withDefaultAccount(it.accounts.firstOrNull())
                )
            }
        }
    }

    // Selects [account] only while the source is still unselected.
    private fun TransactionSide.withDefaultAccount(account: Account?) =
        if (account != null && this.account is AccountRef.Unselected) {
            copy(account = AccountRef.fromStored(account.id), currency = account.currencyCode)
        } else this

    private fun TemplateEntryState.withPrefill(prefill: TemplatePrefillDTO): TemplateEntryState {
        val amount = prefill.amount.abs().stripTrailingZeros().toPlainString()
        return copy(
            isEditMode = false,
            templateId = null,
            name = prefill.name,
            source = TransactionSide(
                account = if (prefill.entryMode == EntryMode.TRANSFER) {
                    AccountRef.fromTransferSide(prefill.accountId)
                } else {
                    AccountRef.fromStored(prefill.accountId)
                },
                amount = amount,
                currency = prefill.currency ?: DEFAULT_CURRENCY,
                categoryId = prefill.categoryId
            ),
            destination = TransactionSide(amount = amount),
            entryMode = prefill.entryMode,
            note = prefill.note ?: "",
            payee = prefill.payee ?: "",
            location = prefill.location ?: "",
            gpsLocation = prefill.gpsLocation,
            warrantyExpirationMillis = prefill.warrantyExpiration ?: defaultWarrantyExpiration(),
            isWarrantyEnabled = prefill.warrantyExpiration != null,
            transactionStatus = prefill.status,
            paymentMethod = prefill.paymentMethod ?: PaymentMethod.CASH,
            selectedTags = prefill.tags
        )
    }

    private fun loadTemplate(templateId: Long) {
        viewModelScope.launch {
            val template = templateRepository.getTemplateById(templateId) ?: return@launch
            val isLinked = plannedPaymentRepository.isTemplateUsedInPlannedPayments(templateId)
            _state.update { it.withTemplate(template, isLinked) }
            updateCategoryName()
        }
    }

    private fun TemplateEntryState.withTemplate(template: Template, isLinked: Boolean): TemplateEntryState {
        val isTransfer = template.entryMode == EntryMode.TRANSFER
        fun accountRef(id: String?) = if (isTransfer) AccountRef.fromTransferSide(id) else AccountRef.fromStored(id)

        val sourceAmount = template.amount.abs()
        return copy(
            templateId = template.id,
            isEditMode = true,
            isLinkedToPlannedPayment = isLinked,
            name = template.name,
            source = TransactionSide(
                account = accountRef(template.accountId),
                amount = sourceAmount.stripTrailingZeros().toPlainString(),
                currency = template.currency,
                // A repeatable row can never be a one-per-currency opening balance/adjustment.
                categoryId = if (isTransfer) Category.SYSTEM_TRANSFER else template.categoryId
            ),
            destination = TransactionSide(
                account = accountRef(template.targetAccountId),
                amount = sourceAmount.multiply(template.transferRate)
                    .setScale(RATE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(),
                currency = template.targetCurrency ?: ""
            ),
            entryMode = template.entryMode,
            exchangeRate = template.transferRate,
            note = template.note ?: "",
            payee = template.payee ?: "",
            location = template.location ?: "",
            gpsLocation = template.gpsLocation,
            warrantyExpirationMillis = template.warrantyExpiration ?: defaultWarrantyExpiration(),
            isWarrantyEnabled = template.warrantyExpiration != null,
            transactionStatus = template.transactionStatus,
            paymentMethod = template.paymentMethod ?: PaymentMethod.CASH,
            selectedTags = template.tags
        )
    }

    private fun defaultWarrantyExpiration(): Long = System.currentTimeMillis() + ONE_YEAR_MILLIS

    // endregion

    // region Events

    fun onEvent(event: TemplateEntryEvent) {
        when (event) {
            is TemplateEntryEvent.SetName -> _state.update { it.withName(event.name) }
            is TemplateEntryEvent.SetAmount -> _state.update { s ->
                s.applyUpdate(
                    TransactionEntryService.handleAmountChange(
                        amountStr = event.amount,
                        currentState = s.toUpdateResult(),
                        exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates)
                    )
                )
            }

            is TemplateEntryEvent.SetTargetAmount -> _state.update { s ->
                s.applyUpdate(
                    TransactionEntryService.handleTargetAmountChange(
                        amountStr = event.amount,
                        currentState = s.toUpdateResult(),
                        exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates)
                    )
                )
            }

            is TemplateEntryEvent.SetTransactionType -> transitionTo(event.type)

            // The transfer categories are chosen by the entry mode, never from the picker.
            is TemplateEntryEvent.SetCategoryId -> if (!Category.isTransfer(event.id)) {
                _state.update { it.copy(source = it.source.copy(categoryId = event.id)) }
                updateCategoryName()
            }

            is TemplateEntryEvent.SetAccountId -> selectAccount(event.id, isSource = true)
            is TemplateEntryEvent.SetTargetAccountId -> selectAccount(event.id, isSource = false)
            is TemplateEntryEvent.SetCurrency -> {
                _state.update { s ->
                    val nextTarget = if (s.isTransfer && s.destination.account.isOutside) event.currency else s.targetCurrency
                    s.copy(
                        source = s.source.copy(currency = event.currency),
                        destination = s.destination.copy(currency = nextTarget)
                    )
                }
                recalculateUnlessRateFixed()
            }

            is TemplateEntryEvent.SetTargetCurrency -> {
                _state.update { s ->
                    val nextSource = if (s.isTransfer && s.source.account.isOutside) event.currency else s.currency
                    s.copy(
                        destination = s.destination.copy(currency = event.currency),
                        source = s.source.copy(currency = nextSource)
                    )
                }
                recalculateUnlessRateFixed()
            }

            is TemplateEntryEvent.SetExchangeRate -> _state.update { it.withManualRate(event.rate) }
            is TemplateEntryEvent.SetNote -> _state.update { it.copy(note = event.note) }
            is TemplateEntryEvent.SetPayee -> _state.update { it.copy(payee = event.payee) }
            is TemplateEntryEvent.SetLocation -> _state.update {
                it.copy(gpsLocation = event.gpsLocation, location = event.location ?: "")
            }

            is TemplateEntryEvent.SetWarrantyEnabled -> _state.update { it.copy(isWarrantyEnabled = event.enabled) }
            is TemplateEntryEvent.SetWarrantyExpiration -> _state.update {
                it.copy(warrantyExpirationMillis = event.millis, showWarrantyDatePicker = false)
            }

            is TemplateEntryEvent.SetTransactionStatus -> _state.update { it.copy(transactionStatus = event.status) }
            is TemplateEntryEvent.SetPaymentMethod -> _state.update { it.copy(paymentMethod = event.method) }
            is TemplateEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is TemplateEntryEvent.SetLinkedTransactionIds -> setLinkedTransactions(event.ids)
            is TemplateEntryEvent.RemoveLinkedTransaction -> _state.update {
                it.copy(
                    displayLinkedTxs = it.displayLinkedTxs.filter { tx -> tx.id != event.targetId },
                    linkedTransactionIds = it.linkedTransactionIds.filter { id -> id != event.targetId }
                )
            }

            is TemplateEntryEvent.AddTag -> viewModelScope.launch {
                tagRepository.insertTag(Tag(name = event.name, colorHex = event.color ?: -1))
            }

            is TemplateEntryEvent.UpdateTag -> viewModelScope.launch {
                tagRepository.updateTag(event.tag.name, event.tag)
            }

            is TemplateEntryEvent.DeleteTag -> viewModelScope.launch { tagRepository.deleteTag(event.tag) }

            is TemplateEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is TemplateEntryEvent.ShowTargetAccountSheet -> _state.update { it.copy(showTargetAccountSheet = event.show) }
            is TemplateEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is TemplateEntryEvent.ShowPaymentMethodSheet -> _state.update { it.copy(showPaymentMethodSheet = event.show) }
            is TemplateEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is TemplateEntryEvent.ShowCurrencySheet -> _state.update { it.copy(showCurrencySheet = event.show) }
            is TemplateEntryEvent.ShowTargetCurrencySheet ->
                _state.update { it.copy(showTargetCurrencySheet = event.show) }
            is TemplateEntryEvent.ShowExchangeRateDialog -> _state.update { it.copy(showExchangeRateDialog = event.show) }
            is TemplateEntryEvent.ShowWarrantyDatePicker -> _state.update { it.copy(showWarrantyDatePicker = event.show) }
            is TemplateEntryEvent.ShowLocationSheet -> _state.update { it.copy(showLocationSheet = event.show) }
            is TemplateEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is TemplateEntryEvent.ShowLinkedTransactionSheet ->
                _state.update { it.copy(showLinkedTransactionSheet = event.show) }
            is TemplateEntryEvent.ShowNumpad -> _state.update {
                it.copy(isNumpadVisible = event.show, activeAmountSide = event.side)
            }
            is TemplateEntryEvent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = event.show) }
            is TemplateEntryEvent.DismissAllSheets -> _state.update { it.withAllSheetsDismissed() }

            is TemplateEntryEvent.SwapAccounts -> {
                _state.update { s ->
                    if (!s.isTransfer) return@update s
                    val (newSource, newDestination, newRate) = TransactionEntryService.swapAccounts(
                        source = s.source,
                        destination = s.destination,
                        transferRate = s.exchangeRate
                    )
                    s.copy(source = newSource, destination = newDestination, exchangeRate = newRate)
                }
                recalculateUnlessRateFixed()
            }

            is TemplateEntryEvent.ToggleRateMode -> {
                _state.update { it.copy(isRateFixed = !it.isRateFixed) }
                recalculateUnlessRateFixed()
            }

            is TemplateEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    // Keeps the auto-generated "<name> - " note prefix in step with the name until the user edits it.
    private fun TemplateEntryState.withName(newName: String): TemplateEntryState {
        val autoPrefix = "$name - "
        val newNote = when {
            note.isEmpty() -> "$newName - "
            name.isNotEmpty() && note.startsWith(autoPrefix) -> note.replaceFirst(autoPrefix, "$newName - ")
            else -> note
        }
        return copy(name = newName, note = newNote)
    }

    // Setting a rate by hand always pins it.
    private fun TemplateEntryState.withManualRate(rate: BigDecimal): TemplateEntryState {
        val sync = TransactionEntryService.syncTransferState(
            sourceAmount = source.amountValue,
            targetAmount = TransactionEntryService.calculateTargetAmountValue(source.amountValue, rate),
            sourceCurrency = currency,
            targetCurrency = targetCurrency,
            isFixed = true,
            activeSide = AmountEditingSide.SOURCE,
            exchangeRates = ExchangeRateSnapshot(currency, exchangeRates)
        )
        return copy(
            isRateFixed = true,
            exchangeRate = rate,
            destination = destination.copy(amount = sync.targetAmount.stripTrailingZeros().toPlainString())
        )
    }

    private fun TemplateEntryState.withAllSheetsDismissed() = copy(
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
        showLinkedTransactionSheet = false
    )

    private fun transitionTo(newMode: EntryMode) {
        val oldMode = _state.value.entryMode
        _state.update { state ->
            val result = TransactionEntryService.transitionType(
                oldMode = oldMode,
                newMode = newMode,
                currentSource = state.source,
                currentDestination = state.destination,
                isSplitMode = false // Templates don't support split mode
            )
            state.copy(entryMode = result.newMode, source = result.source, destination = result.destination)
        }
        if (newMode == EntryMode.TRANSFER) recalculateExchangeRate()
    }

    private fun selectAccount(accountId: String?, isSource: Boolean) {
        _state.update { s ->
            s.applyUpdate(
                TransactionEntryService.handleAccountSelection(
                    accountId = accountId,
                    isSource = isSource,
                    currentState = s.toUpdateResult(),
                    accounts = s.accounts,
                    baseCurrency = s.accounts.firstOrNull()?.currencyCode ?: DEFAULT_CURRENCY
                )
            )
        }
        recalculateUnlessRateFixed()
    }

    private fun recalculateUnlessRateFixed() {
        if (!_state.value.isRateFixed) recalculateExchangeRate()
    }

    private fun setLinkedTransactions(ids: List<Long>) {
        viewModelScope.launch {
            withProfile { profileId ->
                val transactions = ids.mapNotNull { transactionUseCases.getTransactionById(it, profileId) }
                _state.update { it.copy(displayLinkedTxs = transactions, linkedTransactionIds = ids) }
            }
        }
    }

    private fun updateCategoryName() {
        _state.update { state ->
            val category = state.categories.find { it.id == state.selectedCategoryId }
            state.copy(categoryName = category?.name ?: "Unknown", selectedCategory = category)
        }
    }

    // endregion

    // region Save, delete, search

    fun saveTemplate(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val request = TransactionEntryService.createRequest(
                name = s.name,
                amount = s.amountValue,
                entryMode = s.entryMode,
                sourceAccount = s.source.account,
                sourceCurrency = s.currency,
                targetAccount = s.destination.account,
                targetCurrency = s.targetCurrency,
                categoryId = if (s.isTransfer) Category.SYSTEM_TRANSFER else s.selectedCategoryId,
                note = s.note,
                payee = s.payee,
                tags = s.selectedTags,
                status = s.transactionStatus,
                targetAmountOverride = if (s.isTransfer) s.destination.amountValue else null,
                exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates),
                accounts = s.accounts,
                baseCurrency = settingsRepository.currency.first()
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

            if (s.isEditMode) templateRepository.updateTemplate(template) else templateRepository.insertTemplate(template)
            onSuccess()
        }
    }

    fun deleteTemplate() {
        val id = _state.value.templateId ?: return
        viewModelScope.launch {
            templateRepository.getTemplateById(id)?.let { templateRepository.deleteTemplate(it) }
            _state.update { it.copy(navTriggered = true) }
        }
    }

    /** Search transactions for linking. */
    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
        val profileId = settingsRepository.currentProfileId.first()
        emitAll(transactionUseCases.searchTransactions(profileId, query))
    }

    // endregion

    private companion object {
        const val DEFAULT_CURRENCY = "USD"
        const val RATE_SCALE = 8
        const val ONE_YEAR_MILLIS = 365L * 24 * 60 * 60 * 1000
    }
}
