package com.osscabinet.ossWallet.ui.screens.filters.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.entry.EntryField

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
    val matchAllTags: Boolean = true,
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
    val timelineRange: TimelineRange = TimelineRange.AllTime(excludeFuture = true),

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
    val existingFilter: Filter? = null,
    val shortlistedCurrencies: List<String> = emptyList()
) {
    val errors: Set<EntryField>
        get() = buildSet {
            if (name.isBlank()) add(EntryField.NAME)
        }

    val hasError: Boolean get() = errors.isNotEmpty()

    val canSave: Boolean
        get() = !hasError && !navTriggered

    fun toTransactionFilter(): TransactionFilter = TransactionFilter(
        transactionTypes = selectedTransactionTypes,
        accountIds = selectedAccountIds,
        categoryIds = selectedCategoryIds,
        paymentMethods = selectedPaymentMethods,
        statuses = selectedStatuses,
        linkedTransactionIds = linkedTransactionIds,
        tags = selectedTags,
        matchAllTags = matchAllTags,
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
        timelineRange = timelineRange
    )
}
