package com.osscabinet.ossWallet.ui.screens.templates.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import java.math.BigDecimal

/**
 * Immutable state holder for template entry form.
 */
data class TemplateEntryState(
    // Core form fields
    val name: String = "",
    val amount: String = "",
    val transactionType: Int = 0, // 0: Expense, 1: Income, 2: Transfer
    val categoryId: Long = 1002L,
    val categoryName: String = "Unknown",
    val accountId: String? = null,
    val targetAccountId: String? = null,
    val currency: String = "USD",
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE,
    val note: String = "",
    val payee: String = "",
    val location: String = "",
    val paymentMethod: String = "Cash",
    val tags: List<String> = emptyList(),

    // UI State
    val showAccountSheet: Boolean = false,
    val showTargetAccountSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showCurrencySheet: Boolean = false,
    val showTargetCurrencySheet: Boolean = false,
    val showExchangeRateDialog: Boolean = false,
    val isNumpadVisible: Boolean = false,

    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val uniquePayees: List<String> = emptyList(),
    val uniqueNotes: List<String> = emptyList(),
    val decimalPrecision: Int = 2,

    // Metadata
    val templateId: Long? = null,
    val isEditMode: Boolean = false,
    val navTriggered: Boolean = false,
    val prefillDto: TransactionPrefillDTO? = null
) {
    val selectedAccount: Account? get() = accounts.find { it.id == accountId }
    val selectedTargetAccount: Account? get() = accounts.find { it.id == targetAccountId }
    val selectedCategory: Category? get() = categories.find { it.id == categoryId }

    val amountValue: BigDecimal
        get() = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO

    val canSave: Boolean
        get() = name.isNotBlank()
}

sealed class TemplateEntryEvent {
    data class SetName(val name: String) : TemplateEntryEvent()
    data class SetAmount(val amount: String) : TemplateEntryEvent()
    data class SetTransactionType(val type: Int) : TemplateEntryEvent()
    data class SetCategory(val categoryId: Long) : TemplateEntryEvent()
    data class SetAccount(val accountId: String?) : TemplateEntryEvent()
    data class SetTargetAccount(val accountId: String?) : TemplateEntryEvent()
    data class SetCurrency(val currency: String) : TemplateEntryEvent()
    data class SetTargetCurrency(val currency: String?) : TemplateEntryEvent()
    data class SetTransferRate(val rate: BigDecimal) : TemplateEntryEvent()
    data class SetNote(val note: String) : TemplateEntryEvent()
    data class SetPayee(val payee: String) : TemplateEntryEvent()
    data class SetLocation(val location: String) : TemplateEntryEvent()
    data class SetPaymentMethod(val method: String) : TemplateEntryEvent()
    data class SetTags(val tags: List<String>) : TemplateEntryEvent()
    data class AddTag(val name: String, val color: Int? = null) : TemplateEntryEvent()
    data class UpdateTag(val tag: Tag) : TemplateEntryEvent()
    data class DeleteTag(val tag: Tag) : TemplateEntryEvent()

    // Sheet visibility
    data class ShowAccountSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowTargetAccountSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowCategorySheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowPaymentMethodSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowTagSheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowCurrencySheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowTargetCurrencySheet(val show: Boolean) : TemplateEntryEvent()
    data class ShowExchangeRateDialog(val show: Boolean) : TemplateEntryEvent()
    data class ShowNumpad(val show: Boolean) : TemplateEntryEvent()

    // Navigation
    data object TriggerNavigation : TemplateEntryEvent()
}
