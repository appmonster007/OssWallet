package com.osscabinet.ossWallet.ui.screens.transactions.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import java.math.BigDecimal

/**
 * Sealed class representing events that can modify the transaction entry state.
 */
sealed class TransactionEntryEvent {
    data class SetAmount(val amount: String) : TransactionEntryEvent()
    data class SetCategory(val categoryId: Long) : TransactionEntryEvent()
    data class SetTags(val tags: Set<String>) : TransactionEntryEvent()
    data class AddTag(val tag: String, val color: Int? = null) : TransactionEntryEvent()
    data class DeleteTag(val tag: Tag) : TransactionEntryEvent()
    data class RemoveTag(val tag: String) : TransactionEntryEvent()
    data class SetPaymentMethod(val method: PaymentMethod) : TransactionEntryEvent()
    data class SetStatus(val status: TransactionStatus) : TransactionEntryEvent()
    data class SetTransactionType(val type: TransactionType) : TransactionEntryEvent()
    data class SetDate(val millis: Long) : TransactionEntryEvent()
    data class SetPayee(val payee: String) : TransactionEntryEvent()
    data class SetNote(val note: String) : TransactionEntryEvent()
    data class SetWarrantyEnabled(val enabled: Boolean) : TransactionEntryEvent()
    data class SetWarrantyExpiration(val millis: Long) : TransactionEntryEvent()
    data class SetLocation(val gpsLocation: String?, val location: String? = null) :
        TransactionEntryEvent()

    data class AddLinkedTransaction(val id: Long) : TransactionEntryEvent()
    data class RemoveLinkedTransaction(val transactionId: Long, val linkedTransactionId: Long) :
        TransactionEntryEvent()

    data class SetLinkedTransactionIds(val ids: List<Long>) : TransactionEntryEvent()
    data class SetAccount(val account: Account?) : TransactionEntryEvent()
    data class SetCurrency(val currency: String) : TransactionEntryEvent()
    data class SetTargetAccount(val account: Account?) : TransactionEntryEvent()
    data class SetTargetCurrency(val currency: String) : TransactionEntryEvent()
    data class SetExchangeRate(val rate: BigDecimal) : TransactionEntryEvent()
    data object SwapAccounts : TransactionEntryEvent()
    data class SetTemplate(val templateName: String?) : TransactionEntryEvent()
    data class ApplyTemplate(val template: Template) : TransactionEntryEvent()
    data class ApplyPrefill(val prefill: TransactionPrefillDTO) : TransactionEntryEvent()

    // Splitting
    data class ToggleSplitMode(val enabled: Boolean) : TransactionEntryEvent()
    data object AddSplit : TransactionEntryEvent()
    data class RemoveSplit(val index: Int) : TransactionEntryEvent()
    data class UpdateSplit(
        val index: Int,
        val categoryId: Long? = null,
        val amount: String? = null,
        val note: String? = null
    ) : TransactionEntryEvent()

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
    data class ShowSplitSheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowSplitCategorySheet(val splitIndex: Int?) : TransactionEntryEvent()
    data class ShowNumpad(val show: Boolean) : TransactionEntryEvent()
    data class ShowCurrencySheet(val show: Boolean) : TransactionEntryEvent()
    data class ShowTargetCurrencySheet(val show: Boolean) : TransactionEntryEvent()

    // Navigation
    data object TriggerNavigation : TransactionEntryEvent()
    data object ClearNavigationTrigger : TransactionEntryEvent()
}
