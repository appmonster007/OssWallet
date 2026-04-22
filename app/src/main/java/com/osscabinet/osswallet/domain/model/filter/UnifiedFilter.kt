package com.osscabinet.ossWallet.domain.model.filter

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Unified filter model that supports atomic filters, composite filters, and saved filter references.
 * This provides a standard format for all filtering operations across the application.
 */
@Serializable
sealed class UnifiedFilter {

    // ========== Atomic Filters (Single Criteria) ==========

    /**
     * Filter transactions by text content (searches both note and payee fields).
     * This provides a unified search experience without requiring users to specify which field to search.
     * By default, the search is case-insensitive for better user experience.
     */
    @Serializable
    data class TextFilter(
        val query: String,
        val isRegex: Boolean = false,
        val isCaseInsensitive: Boolean = true,
        val operator: FilterOperator = FilterOperator.CONTAINS
    ) : UnifiedFilter()

    /**
     * Filter transactions by category IDs.
     */
    @Serializable
    data class CategoryFilter(
        val categoryIds: List<Long>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()

    /**
     * Filter transactions by account IDs.
     */
    @Serializable
    data class AccountFilter(
        val accountIds: List<String>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()

    /**
     * Filter transactions by timeline/date range.
     */
    @Serializable
    data class DateRangeFilter(
        val timelineRange: TimelineRange
    ) : UnifiedFilter()

    /**
     * Filter transactions by amount range.
     * BigDecimal is serialized as string to preserve precision.
     */
    @Serializable
    data class AmountFilter(
        val min: BigDecimal? = null,
        val max: BigDecimal? = null
    ) : UnifiedFilter()

    /**
     * Filter transactions by payment methods.
     */
    @Serializable
    data class PaymentMethodFilter(
        val methods: List<String>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()

    /**
     * Filter transactions by currencies.
     */
    @Serializable
    data class CurrencyFilter(
        val currencies: List<String>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()

    /**
     * Filter transactions by income/expense type.
     */
    @Serializable
    data class IncomeTypeFilter(
        val isIncome: Boolean? = null
    ) : UnifiedFilter()

    /**
     * Filter transactions by warranty status and expiration.
     */
    @Serializable
    data class WarrantyFilter(
        val hasWarranty: Boolean? = null,
        val expirationStart: Long? = null,
        val expirationEnd: Long? = null
    ) : UnifiedFilter()

    // ========== Composite Filters (Combine Multiple Filters) ==========

    /**
     * Combines multiple filters with a logical operator (AND/OR).
     */
    @Serializable
    data class CompositeFilter(
        val filters: List<UnifiedFilter>,
        val operator: LogicalOperator = LogicalOperator.AND
    ) : UnifiedFilter()

    // ========== Saved Filter Reference ==========

    /**
     * Reference to a saved filter by ID.
     * Allows combining saved filters with ad-hoc filters.
     */
    @Serializable
    data class SavedFilterReference(
        val filterId: Long
    ) : UnifiedFilter()

    // ========== Instant/Transient Filters ==========

    /**
     * Wraps any filter to mark it as transient/instant.
     * Instant filters are not persisted and only exist until screen changes.
     * These are useful for quick, temporary filtering without saving.
     */
    @Serializable
    data class InstantFilter(
        val filter: UnifiedFilter
    ) : UnifiedFilter()
}
