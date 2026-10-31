package com.osscabinet.ossWallet.ui.screens.budgets.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.entry.EntryField

data class BudgetEntryState(
    val name: String = "",
    val limitAmount: String = "",
    val currency: String = "USD",
    val timelineRange: TimelineRange = TimelineRange.ThisPeriod(com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType.MONTH),
    val selectedTrackTypes: Set<TransactionType> = setOf(
        TransactionType.EXPENSE,
        TransactionType.INCOME
    ),
    val alertThreshold: Float = 0.8f,
    val rollover: Boolean = false,
    val note: String = "",
    val queryNote: String = "",
    val isNoteRegex: Boolean = false,
    val queryPayee: String = "",
    val isPayeeRegex: Boolean = false,
    val minAmount: String = "",
    val maxAmount: String = "",
    val hasWarranty: Boolean? = null,
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedAccountIds: Set<String> = emptySet(),
    val selectedCurrencies: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val matchAllTags: Boolean = true,
    val selectedPaymentMethods: Set<PaymentMethod> = emptySet(),
    val selectedStatuses: Set<TransactionStatus> = emptySet(),
    val linkedTransactionIds: Set<Long> = emptySet(),
    val filterId: Long? = null,
    val linkedFilter: Filter? = null,
    val filterUsageCount: Int = 0,
    val showSharedFilterDialog: Boolean = false,
    val deleteFilterWithBudget: Boolean = false,
    val showDeleteDialog: Boolean = false,

    // UI State
    val showCustomStartPicker: Boolean = false,
    val showCustomEndPicker: Boolean = false,
    val showNumpad: Boolean = false,
    val showBaseCurrencySheet: Boolean = false,
    val showTimelineSheet: Boolean = false,
    val showScheduleBuilder: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showTrackTypeSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showTagSheet: Boolean = false,
    val showCurrencyFilterSheet: Boolean = false,
    val showPaymentMethodSheet: Boolean = false,
    val showStatusSheet: Boolean = false,
    val showLinkSheet: Boolean = false,
    val showFilterSheet: Boolean = false,
    val showMinAmountSheet: Boolean = false,
    val showMaxAmountSheet: Boolean = false,
    val showWarrantyStartPicker: Boolean = false,
    val showWarrantyEndPicker: Boolean = false,
    val accountQuery: String = "",
    val categoryQuery: String = "",
    val isEditMode: Boolean = false,
    val budgetId: Long? = null,
    val navTriggered: Boolean = false,

    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val allPayees: List<String> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val uniqueNotes: List<String> = emptyList(),
    val currentProfileId: Int = 1,
    val existingBudget: Budget? = null,
    val availableFilters: List<Filter> = emptyList(),
    val shortlistedCurrencies: List<String> = emptyList()
) {
    val errors: Set<EntryField>
        get() = buildSet {
            if (name.isBlank()) add(EntryField.NAME)
            if (limitAmount.toBigDecimalOrNull() == null) add(EntryField.AMOUNT)
        }

    val hasError: Boolean get() = errors.isNotEmpty()

    val canSave: Boolean
        get() = !hasError && !navTriggered
}
