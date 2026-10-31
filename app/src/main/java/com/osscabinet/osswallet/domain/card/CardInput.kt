package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.service.category.CategoryHierarchyCache
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot

/**
 * The ONE shared input for all card computations.
 * Wraps all raw financial data needed for any card.
 *
 * [filter] represents the BASE scope of the card (e.g. fixed to an account).
 * This filter is applied BEFORE the card-specific user-adjustable filter.
 */
data class CardInput(
    val profileId: Int = 0,
    val transactionsVersion: Long = 0L,
    val accounts: List<Account>,
    val categories: List<Category>,
    val tags: List<com.osscabinet.ossWallet.domain.model.Tag> = emptyList(),
    val exchangeRates: ExchangeRateSnapshot,
    val targetCurrency: String,
    val filter: TransactionFilter = TransactionFilter(),
    val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val commonFilterEnabled: Boolean = false,
    val plannedPayments: List<PlannedPayment> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val goals: List<com.osscabinet.ossWallet.domain.model.Goal> = emptyList(),
    val loanPayments: Map<Long, List<LoanPayment>> = emptyMap(),
    // Pre-computed hierarchy — built once in ComputeCardUseCase before dispatching to all cards.
    // Default is Empty; ComputeCardUseCase always enriches it before compute.
    val categoryHierarchy: CategoryHierarchyCache = CategoryHierarchyCache.Empty,
    val deleteEnabled: Boolean = true
) {
    /**
     * Derived property that determines if the inherited filter's timeline
     * is authoritative based on the screen context and user settings.
     */
    val screenOwnsTimeline: Boolean get() = screenContext.ownsTimeline(commonFilterEnabled)

    val effectiveAccounts: List<Account> = accounts.filter { !it.isArchived }

    val effectiveAccountIds: Set<String> = effectiveAccounts.mapTo(HashSet()) { it.id }
}
