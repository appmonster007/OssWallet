package com.osscabinet.ossWallet.ui.screens.transactions.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import java.math.BigDecimal

/**
 * Immutable state holder for transaction entry form.
 * All UI state is centralized here for predictable state management.
 */
data class TransactionEntryState(
    // Core form fields
    val title: String = "",
    val amount: String = "",
    val category: String = "Shopping",
    val selectedCategoryId: Long? = null,
    val selectedTags: List<String> = emptyList(),
    val paymentMethod: String = "Cash",
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val transactionType: Int = 0, // 0: Expense, 1: Income, 2: Transfer
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val payee: String = "",
    val note: String = "",

    // Warranty
    val isWarrantyEnabled: Boolean = false,
    val warrantyExpirationMillis: Long = System.currentTimeMillis() + 31536000000L, // +1 year

    // Location
    val locationCoords: Pair<Double, Double>? = null,

    // Linked transactions
    val linkedTransactionIds: List<Long> = emptyList(),

    // Account selection
    val selectedAccount: Account? = null,
    val selectedCurrency: String = "",

    // Transfer specific
    val targetAccount: Account? = null,
    val targetCurrency: String = "",
    val exchangeRate: BigDecimal = BigDecimal.ONE,

    // Template
    val selectedTemplateName: String? = null,

    // Sheet visibility flags
    val showDatePicker: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showTargetAccountSheet: Boolean = false,
    val showExchangeRateDialog: Boolean = false,
    val showTemplateSheet: Boolean = false,
    val showLinkedTransactionSheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
    val showWarrantyDatePicker: Boolean = false,
    val showLocationSheet: Boolean = false,
    val isNumpadVisible: Boolean = false,

    // External data (populated from repositories)
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val templates: List<Template> = emptyList(),
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
    val decimalPrecision: Int = 2,
    val use24HourFormat: Boolean = false,

    // Edit mode data
    val transactionId: Long? = null,
    val existingTransaction: Transaction? = null,
    val linkedTxs: List<Transaction> = emptyList(),
    val prefillDto: TransactionPrefillDTO? = null,

    // Navigation
    val isEditMode: Boolean = false,
    val navTriggered: Boolean = false
) {
    val isIncome: Boolean get() = transactionType == 1
    val isExpense: Boolean get() = transactionType == 0
    val isTransfer: Boolean get() = transactionType == 2

    val displayLinkedTxs: List<Transaction>
        get() = if (isEditMode) {
            linkedTxs.sortedByDescending { it.timestamp }
        } else {
            linkedTransactionIds.mapNotNull { id -> transactions.find { it.id == id } }
                .sortedByDescending { it.timestamp }
        }

    val amountValue: BigDecimal
        get() = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO

    val canSave: Boolean
        get() = amountValue > BigDecimal.ZERO && selectedAccount != null
}

/**
 * Sealed class representing events that can modify the transaction entry state.
 */
sealed class TransactionEntryEvent {
    data class SetTitle(val title: String) : TransactionEntryEvent()
    data class SetAmount(val amount: String) : TransactionEntryEvent()
    data class SetCategory(val category: String, val categoryId: Long?) : TransactionEntryEvent()
    data class SetTags(val tags: List<String>) : TransactionEntryEvent()
    data class AddTag(val tag: String, val color: Int? = null) : TransactionEntryEvent()
    data class DeleteTag(val tag: Tag) : TransactionEntryEvent()
    data class RemoveTag(val tag: String) : TransactionEntryEvent()
    data class SetPaymentMethod(val method: String) : TransactionEntryEvent()
    data class SetStatus(val status: TransactionStatus) : TransactionEntryEvent()
    data class SetTransactionType(val type: Int) : TransactionEntryEvent()
    data class SetDate(val millis: Long) : TransactionEntryEvent()
    data class SetPayee(val payee: String) : TransactionEntryEvent()
    data class SetNote(val note: String) : TransactionEntryEvent()
    data class SetWarrantyEnabled(val enabled: Boolean) : TransactionEntryEvent()
    data class SetWarrantyExpiration(val millis: Long) : TransactionEntryEvent()
    data class SetLocation(val coords: Pair<Double, Double>?) : TransactionEntryEvent()
    data class AddLinkedTransaction(val id: Long) : TransactionEntryEvent()
    data class RemoveLinkedTransaction(val transactionId: Long, val linkedTransactionId: Long) : TransactionEntryEvent()
    data class SetLinkedTransactionIds(val ids: List<Long>) : TransactionEntryEvent()
    data class SetAccount(val account: Account?) : TransactionEntryEvent()
    data class SetCurrency(val currency: String) : TransactionEntryEvent()
    data class SetTargetAccount(val account: Account?) : TransactionEntryEvent()
    data class SetTargetCurrency(val currency: String) : TransactionEntryEvent()
    data class SetExchangeRate(val rate: BigDecimal) : TransactionEntryEvent()
    data class SetTemplate(val templateName: String?) : TransactionEntryEvent()
    data class ApplyTemplate(val template: Template) : TransactionEntryEvent()

    // Sheet visibility
    data class ShowDatePicker(val show: Boolean) : TransactionEntryEvent()
    data class ShowCategorySheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowAccountSheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowTargetAccountSheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowExchangeRateDialog(val show: Boolean) : TransactionEntryEvent()
    data class ShowTemplateSheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowLinkedTransactionSheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowTagSheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowPaymentMethodSheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowStatusSheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowWarrantyDatePicker(val show: Boolean) : TransactionEntryEvent()
    data class ShowLocationSheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowNumpad(val show: Boolean) : TransactionEntryEvent()

    // Navigation
    data object TriggerNavigation : TransactionEntryEvent()
    data object ClearNavigationTrigger : TransactionEntryEvent()
}
