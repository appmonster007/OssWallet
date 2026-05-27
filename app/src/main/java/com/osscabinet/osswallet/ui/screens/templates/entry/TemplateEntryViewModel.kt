package com.osscabinet.ossWallet.ui.screens.templates.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TemplatePrefillDTO
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TemplateEntryViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val transactionUseCases: TransactionUseCases,
    private val computationRepository: ComputationRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(TemplateEntryState())
    val state: StateFlow<TemplateEntryState> = _state.asStateFlow()

    init {
        loadInitialData()
        viewModelScope.launch {
            effectiveShortlistedCurrencies.collect { currencies ->
                _state.update { it.copy(shortlistedCurrencies = currencies) }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { profileId ->
                combine(
                    accountRepository.getAllAccounts(profileId),
                    categoryRepository.categories,
                    tagRepository.getAllTags()
                ) { accounts, categories, tags ->
                    val payees = transactionUseCases.getUniquePayees(profileId)
                    val notes = transactionUseCases.getUniqueNotes(profileId)

                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            allTags = tags,
                            uniquePayees = payees,
                            uniqueNotes = notes
                        )
                    }
                }
            }.collect()
        }
    }

    fun initialize(templateId: Long? = null, prefill: TemplatePrefillDTO? = null) {
        if (templateId == null) {
            if (prefill != null) {
                _state.update {
                    it.copy(
                        isEditMode = false,
                        templateId = null,
                        name = prefill.name,
                        source = TransactionSide(
                            accountId = prefill.accountId,
                            amount = prefill.amount.abs().stripTrailingZeros().toPlainString(),
                            currency = prefill.currency ?: "USD",
                            categoryId = prefill.categoryId
                        ),
                        destination = TransactionSide(
                            amount = prefill.amount.abs().stripTrailingZeros().toPlainString()
                        ),
                        transactionType = prefill.transactionType,
                        note = prefill.note ?: "",
                        payee = prefill.payee ?: "",
                        location = prefill.location ?: "",
                        gpsLocation = prefill.gpsLocation,
                        warrantyExpirationMillis = prefill.warrantyExpiration
                            ?: (System.currentTimeMillis() + 31536000000L),
                        isWarrantyEnabled = prefill.warrantyExpiration != null,
                        transactionStatus = prefill.status,
                        paymentMethod = prefill.paymentMethod ?: PaymentMethod.CASH,
                        tags = prefill.tags
                    )
                }
                updateCategoryName()
            } else {
                _state.update { it.copy(isEditMode = false, templateId = null) }
            }
            return
        }

        viewModelScope.launch {
            templateRepository.getTemplateById(templateId)?.let { template ->
                val isLinked = plannedPaymentRepository.isTemplateUsedInPlannedPayments(templateId)

                val isOpenedAsReceiver = Category.isAnyTransferInflow(template.categoryId)
                var sourceAccId = template.accountId
                var targetAccId = template.targetAccountId
                var sourceCurrency = template.currency ?: "USD"
                var targetCurr = template.targetCurrency ?: ""
                var sourceAmtStr = template.amount.abs().stripTrailingZeros().toPlainString()
                var targetAmtStr = template.amount.abs().multiply(template.transferRate)
                    .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                    .toPlainString()
                var existingCategoryId = template.categoryId

                if (template.transactionType == TransactionType.TRANSFER && isOpenedAsReceiver) {
                    // Flip for Source-Target consistency
                    sourceAccId = template.targetAccountId
                    targetAccId = template.accountId
                    sourceCurrency = template.targetCurrency ?: template.currency ?: "USD"
                    targetCurr = template.currency ?: ""
                    targetAmtStr = template.amount.abs().stripTrailingZeros().toPlainString()
                    sourceAmtStr = if (template.transferRate.signum() != 0) {
                        template.amount.abs()
                            .divide(template.transferRate, 8, java.math.RoundingMode.HALF_UP)
                            .stripTrailingZeros().toPlainString()
                    } else targetAmtStr
                    // Normalize category for Receiver focus
                    existingCategoryId =
                        if (Category.isInitialBalance(template.categoryId)) Category.SYSTEM_INITIAL_BALANCE_INFLOW else Category.SYSTEM_TRANSFER_INFLOW
                } else if (template.transactionType == TransactionType.TRANSFER) {
                    existingCategoryId =
                        if (Category.isInitialBalance(template.categoryId)) Category.SYSTEM_INITIAL_BALANCE_OUTFLOW else Category.SYSTEM_TRANSFER_OUTFLOW
                }

                _state.update {
                    it.copy(
                        templateId = templateId,
                        isEditMode = true,
                        isLinkedToPlannedPayment = isLinked,
                        name = template.name,
                        source = TransactionSide(
                            accountId = sourceAccId,
                            amount = sourceAmtStr,
                            currency = sourceCurrency,
                            categoryId = existingCategoryId
                        ),
                        destination = TransactionSide(
                            accountId = targetAccId,
                            amount = targetAmtStr,
                            currency = targetCurr
                        ),
                        transactionType = template.transactionType,
                        transferRate = template.transferRate,
                        note = template.note ?: "",
                        payee = template.payee ?: "",
                        location = template.location ?: "",
                        gpsLocation = template.gpsLocation,
                        warrantyExpirationMillis = template.warrantyExpiration
                            ?: (System.currentTimeMillis() + 31536000000L),
                        isWarrantyEnabled = template.warrantyExpiration != null,
                        transactionStatus = template.transactionStatus,
                        paymentMethod = template.paymentMethod ?: PaymentMethod.CASH,
                        tags = template.tags
                    )
                }
                updateCategoryName()
            }
        }
    }

    fun onEvent(event: TemplateEntryEvent) {
        when (event) {
            is TemplateEntryEvent.SetName -> _state.update { it.copy(name = event.name) }
            is TemplateEntryEvent.SetAmount -> {
                val inputAmount = event.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (inputAmount < BigDecimal.ZERO) {
                    _state.update { state ->
                        val newType = when (state.transactionType) {
                            TransactionType.EXPENSE -> TransactionType.INCOME
                            TransactionType.INCOME -> TransactionType.EXPENSE
                            else -> state.transactionType
                        }
                        val finalAmt = inputAmount.abs()
                        state.copy(
                            source = state.source.copy(
                                amount = finalAmt.stripTrailingZeros().toPlainString()
                            ),
                            destination = state.destination.copy(
                                amount = finalAmt.multiply(state.transferRate)
                                    .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                                    .toPlainString()
                            ),
                            transactionType = newType
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            source = it.source.copy(amount = event.amount),
                            destination = it.destination.copy(
                                amount = inputAmount.multiply(it.transferRate)
                                    .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                                    .toPlainString()
                            )
                        )
                    }
                }
            }

            is TemplateEntryEvent.SetTargetAmount -> {
                val inputAmount = event.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                _state.update { state ->
                    val newSourceAmount = if (state.transferRate.signum() != 0) {
                        inputAmount.divide(state.transferRate, 8, java.math.RoundingMode.HALF_UP)
                            .stripTrailingZeros().toPlainString()
                    } else state.amount

                    state.copy(
                        destination = state.destination.copy(amount = event.amount),
                        source = state.source.copy(amount = newSourceAmount)
                    )
                }
            }

            is TemplateEntryEvent.SetTransactionType -> {
                val oldType = _state.value.transactionType
                val newType = event.type
                _state.update { state ->
                    val isTransferToNormal =
                        oldType == TransactionType.TRANSFER && newType != TransactionType.TRANSFER

                    val primarySide = if (isTransferToNormal && state.isTransferInflow) {
                        state.destination
                    } else {
                        state.source
                    }

                    state.copy(
                        transactionType = newType,
                        source = primarySide.copy(
                            amount = if (primarySide.amount.isEmpty()) "" else primarySide.amountValue.abs()
                                .stripTrailingZeros().toPlainString(),

                            accountId = when {
                                newType == TransactionType.TRANSFER && oldType == TransactionType.INCOME -> null
                                else -> primarySide.accountId
                            },

                            categoryId = when {
                                newType != TransactionType.TRANSFER && Category.isTransfer(state.categoryId) -> Category.SYSTEM_UNKNOWN
                                newType == TransactionType.TRANSFER && oldType == TransactionType.INCOME -> Category.SYSTEM_TRANSFER_INFLOW
                                newType == TransactionType.TRANSFER && oldType == TransactionType.EXPENSE -> Category.SYSTEM_TRANSFER_OUTFLOW
                                newType == TransactionType.TRANSFER -> state.categoryId.takeIf {
                                    Category.isTransfer(
                                        it
                                    )
                                } ?: Category.SYSTEM_TRANSFER_OUTFLOW

                                else -> state.categoryId
                            }
                        ),

                        destination = when {
                            newType == TransactionType.TRANSFER && oldType == TransactionType.INCOME -> primarySide.copy(
                                categoryId = Category.SYSTEM_TRANSFER_INFLOW
                            )
                            newType == TransactionType.TRANSFER && oldType == TransactionType.EXPENSE -> TransactionSide()
                            newType == TransactionType.TRANSFER -> state.destination
                            else -> TransactionSide()
                        }
                    )
                }
                if (newType == TransactionType.TRANSFER) {
                    recalculateExchangeRate()
                }
            }

            is TemplateEntryEvent.SetCategory -> {
                if (!Category.isInitialBalance(event.categoryId)) {
                    _state.update { it.copy(source = it.source.copy(categoryId = event.categoryId)) }
                    updateCategoryName()
                }
            }

            is TemplateEntryEvent.SetAccount -> {
                _state.update { state ->
                    val account = state.accounts.find { it.id == event.accountId }
                    state.copy(
                        source = state.source.copy(
                            accountId = event.accountId,
                            currency = account?.currencyCode ?: state.currency
                        )
                    )
                }
                recalculateExchangeRate()
            }

            is TemplateEntryEvent.SetTargetAccount -> {
                _state.update { state ->
                    val account = state.accounts.find { it.id == event.accountId }
                    state.copy(
                        destination = state.destination.copy(
                            accountId = event.accountId,
                            currency = account?.currencyCode ?: state.targetCurrency ?: ""
                        )
                    )
                }
                recalculateExchangeRate()
            }

            is TemplateEntryEvent.SetCurrency -> {
                _state.update { it.copy(source = it.source.copy(currency = event.currency)) }
                recalculateExchangeRate()
            }

            is TemplateEntryEvent.SetTargetCurrency -> {
                _state.update { it.copy(destination = it.destination.copy(currency = event.currency)) }
                recalculateExchangeRate()
            }

            is TemplateEntryEvent.SetTransferRate -> _state.update {
                it.copy(
                    transferRate = event.rate,
                    destination = it.destination.copy(
                        amount = it.amountValue.multiply(event.rate)
                            .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                            .toPlainString()
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
                it.copy(
                    warrantyExpirationMillis = event.millis
                )
            }

            is TemplateEntryEvent.SetTransactionStatus -> _state.update { it.copy(transactionStatus = event.status) }
            is TemplateEntryEvent.SetPaymentMethod -> _state.update { it.copy(paymentMethod = event.method) }
            is TemplateEntryEvent.SetTags -> _state.update { it.copy(tags = event.tags) }
            is TemplateEntryEvent.SetLinkedTransactionIds -> {
                viewModelScope.launch {
                    val transactions = event.ids.mapNotNull { id ->
                        transactionUseCases.getTransactionById(id)
                    }
                    _state.update { it.copy(displayLinkedTxs = transactions) }
                }
            }

            is TemplateEntryEvent.RemoveLinkedTransaction -> {
                _state.update { state ->
                    val newList = state.displayLinkedTxs.filter { it.id != event.targetId }
                    state.copy(displayLinkedTxs = newList)
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
            is TemplateEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    private fun recalculateExchangeRate() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.transactionType == TransactionType.TRANSFER &&
                currentState.currency.isNotEmpty() &&
                currentState.targetCurrency?.isNotEmpty() == true
            ) {
                val rates = computationRepository.exchangeRates.first()
                val rate =
                    com.osscabinet.ossWallet.domain.service.currency.CurrencyService.convertAmount(
                        amount = BigDecimal.ONE,
                        fromCurrency = currentState.currency,
                        toCurrency = currentState.targetCurrency ?: currentState.currency,
                        exchangeRates = rates
                    )
                _state.update {
                    it.copy(
                        transferRate = rate,
                        destination = it.destination.copy(
                            amount = it.amountValue.multiply(rate)
                                .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                                .toPlainString()
                        )
                    )
                }
            }
        }
    }

    private fun updateCategoryName() {
        val categoryId = _state.value.categoryId
        val category = _state.value.categories.find { it.id == categoryId }
        _state.update { it.copy(categoryName = category?.name ?: "Unknown") }
    }

    fun saveTemplate(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val amountValue = if (s.isIncome) s.amountValue.abs()
            else if (s.isTransfer) {
                if (s.isTransferInflow) s.amountValue.abs() else s.amountValue.abs().negate()
            } else s.amountValue.abs().negate()

            val templateCategoryId = if (s.isTransfer) {
                if (Category.isInitialBalance(s.categoryId)) Category.SYSTEM_INITIAL_BALANCE_OUTFLOW
                else Category.SYSTEM_TRANSFER_OUTFLOW
            } else s.categoryId

            val template = Template(
                id = s.templateId ?: 0,
                name = s.name,
                categoryId = templateCategoryId,
                amount = amountValue,
                transactionType = s.transactionType,
                accountId = s.accountId,
                targetAccountId = s.targetAccountId,
                targetCurrency = s.targetCurrency,
                transferRate = s.transferRate,
                note = s.note.ifBlank { null },
                payee = s.payee.ifBlank { null },
                location = s.location.ifBlank { null },
                gpsLocation = s.gpsLocation,
                warrantyExpiration = if (s.isWarrantyEnabled) s.warrantyExpirationMillis else null,
                transactionStatus = s.transactionStatus,
                paymentMethod = s.paymentMethod,
                currency = s.currency.ifBlank { "USD" },
                tags = s.tags,
                profileId = settingsRepository.currentProfileId.first()
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
