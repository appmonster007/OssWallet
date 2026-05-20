package com.osscabinet.ossWallet.ui.screens.filters.entry

import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import java.math.BigDecimal

data class FilterEntryState(
    val name: String = "",
    
    // Filter fields
    val selectedTransactionTypes: Set<TransactionType> = emptySet(),
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
    val timelineRange: TimelineRange = TimelineRange.AllTime,
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
    val showTimelineSheet: Boolean = false,
    val showWarrantyStartPicker: Boolean = false,
    val showWarrantyEndPicker: Boolean = false,
    
    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val existingFilter: Filter? = null
) {
    val canSave: Boolean 
        get() = name.isNotBlank() && !navTriggered
    
    fun toTransactionFilter(): TransactionFilter = TransactionFilter(
        transactionTypes = selectedTransactionTypes,
        accountIds = selectedAccountIds,
        categoryIds = selectedCategoryIds,
        paymentMethods = selectedPaymentMethods,
        statuses = selectedStatuses,
        linkedTransactionIds = linkedTransactionIds,
        tags = selectedTags,
        currencies = selectedCurrencies,
        queryNote = queryNote.takeIf { it.isNotBlank() },
        isNoteRegex = isNoteRegex,
        queryPayee = queryPayee.takeIf { it.isNotBlank() },
        isPayeeRegex = isPayeeRegex,
        minAmount = minAmount.toBigDecimalOrNull(),
        maxAmount = maxAmount.toBigDecimalOrNull(),
        hasWarranty = hasWarranty,
        warrantyExpirationStart = warrantyExpirationStart,
        warrantyExpirationEnd = warrantyExpirationEnd,
        timelineRange = timelineRange,
        excludeFuture = excludeFuture
    )
}
