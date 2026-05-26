package com.osscabinet.ossWallet.ui.screens.transactions.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSplit
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import java.math.BigDecimal

/**
 * Immutable state holder for transaction entry form.
 * All UI state is centralized here for predictable state management.
 */
data class TransactionEntryState(
    // Core form fields
    val amount: String = "",
    val selectedCategoryId: Long = Category.SYSTEM_UNKNOWN,
    val selectedTags: Set<String> = emptySet(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val payee: String = "",
    val note: String = "",

    // Warranty
    val isWarrantyEnabled: Boolean = false,
    val warrantyExpirationMillis: Long = System.currentTimeMillis() + 31536000000L, // +1 year

    // Location
    val gpsLocation: String? = null,
    val location: String? = null,

    // Linked transactions
    val linkedTransactionIds: List<Long> = emptyList(),

    // Account selection
    val selectedAccount: Account? = null,
    val selectedCurrency: String = "",

    // Transfer specific
    val targetAccount: Account? = null,
    val targetCurrency: String = "",
    val exchangeRate: BigDecimal = BigDecimal.ONE,
    val isAmountPositive: Boolean = false,

    // Template
    val selectedTemplateName: String? = null,

    // Splitting
    val isSplitMode: Boolean = false,
    val splits: List<TransactionSplit> = emptyList(),

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
    val showSplitSheet: Boolean = false,
    val showSplitCategorySheet: Int? = null,
    val isNumpadVisible: Boolean = false,
    val showCurrencySheet: Boolean = false,
    val showTargetCurrencySheet: Boolean = false,

    // External data (populated from repositories)
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val templates: List<Template> = emptyList(),
    val allPayees: List<String> = emptyList(),
    val allNotes: List<String> = emptyList(),
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
    val mapsAccessEnabled: Boolean = false,
    val shortlistedCurrencies: List<String> = emptyList(),
    val fullProviderCurrencies: List<String> = emptyList(),

    // Edit mode data
    val transactionId: Long? = null,
    val existingTransaction: Transaction? = null,
    val linkedTxs: List<Transaction> = emptyList(),
    val prefillDto: TransactionPrefillDTO? = null,

    // Navigation
    val isEditMode: Boolean = false,
    val navTriggered: Boolean = false
) {
    val isIncome: Boolean get() = transactionType == TransactionType.INCOME
    val isExpense: Boolean get() = transactionType == TransactionType.EXPENSE
    val isTransfer: Boolean get() = transactionType == TransactionType.TRANSFER

    val displayLinkedTxs: List<Transaction>
        get() = linkedTxs.sortedByDescending { it.timestamp }

    val amountValue: BigDecimal
        get() = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO

    val canSave: Boolean
        get() {
            if (isSplitMode) {
                val totalSplitAmount =
                    splits.sumOf { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                return amountValue >= BigDecimal.ZERO &&
                        totalSplitAmount == amountValue &&
                        selectedAccount != null &&
                        splits.all { it.categoryId != null }
            }
            return amountValue >= BigDecimal.ZERO && selectedAccount != null
        }
}

/**
 * Sealed class representing events that can modify the transaction entry state.
 */
