package com.osscabinet.ossWallet.ui.screens.filters.entry

import com.osscabinet.ossWallet.domain.model.*
import java.math.BigDecimal

data class FilterEntryState(
    val name: String = "",
    
    // Filter fields
    val transactionType: TransactionType? = null,
    val selectedAccountIds: Set<String> = emptySet(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedPaymentMethods: Set<PaymentMethod> = emptySet(),
    val selectedStatuses: Set<TransactionStatus> = emptySet(),
    val linkedTransactionIds: Set<Long> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val selectedCurrencies: Set<String> = emptySet(),
    val queryNote: String = "",
    val isNoteRegex: Boolean = false,
    val queryPayee: String = "",
    val isPayeeRegex: Boolean = false,
    val minAmount: String = "",
    val maxAmount: String = "",
    val hasWarranty: Boolean? = null,
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    val excludeFuture: Boolean = true,
    
    // UI State
    val isEditMode: Boolean = false,
    val filterId: Long? = null,
    val navTriggered: Boolean = false,
    
    // Sheet visibility
    val showAccountSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
    val showLinkSheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showCurrencySheet: Boolean = false,
    val showTypeSheet: Boolean = false,
    val showMinAmountSheet: Boolean = false,
    val showMaxAmountSheet: Boolean = false,
    val showWarrantyStartPicker: Boolean = false,
    val showWarrantyEndPicker: Boolean = false,
    
    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val existingFilter: Filter? = null
) {
    val canSave: Boolean get() = name.isNotBlank() && !navTriggered
    
    fun toTransactionFilter(): TransactionFilter = TransactionFilter(
        transactionType = transactionType,
        accountIds = selectedAccountIds.toList(),
        categoryIds = selectedCategoryIds.toList(),
        paymentMethods = selectedPaymentMethods.toList(),
        statuses = selectedStatuses.toList(),
        linkedTransactionIds = linkedTransactionIds.toList(),
        tags = selectedTags.toList(),
        currencies = selectedCurrencies.toList(),
        queryNote = queryNote.takeIf { it.isNotBlank() },
        isNoteRegex = isNoteRegex,
        queryPayee = queryPayee.takeIf { it.isNotBlank() },
        isPayeeRegex = isPayeeRegex,
        minAmount = minAmount.toBigDecimalOrNull(),
        maxAmount = maxAmount.toBigDecimalOrNull(),
        hasWarranty = hasWarranty,
        warrantyExpirationStart = warrantyExpirationStart,
        warrantyExpirationEnd = warrantyExpirationEnd,
        excludeFuture = excludeFuture
    )
}
