package com.osscabinet.ossWallet.ui.screens.templates.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionStatus
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isWarrantyEnabled: Boolean = false,
    val warrantyExpirationMillis: Long = System.currentTimeMillis() + 31536000000L,
    val transactionStatus: TransactionStatus = TransactionStatus.CLEARED,
    val paymentMethod: String = "Cash",
    val tags: List<String> = emptyList(),

    // UI State
    val showAccountSheet: Boolean = false,
    val showTargetAccountSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showCurrencySheet: Boolean = false,
    val showTargetCurrencySheet: Boolean = false,
    val showExchangeRateDialog: Boolean = false,
    val showWarrantyDatePicker: Boolean = false,
    val showLocationSheet: Boolean = false,
    val isNumpadVisible: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isLinkedToPlannedPayment: Boolean = false,

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
