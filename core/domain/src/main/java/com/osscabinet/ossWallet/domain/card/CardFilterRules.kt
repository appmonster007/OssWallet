package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import java.math.BigDecimal

/**
 * Defines how specific fields in a [TransactionFilter] are constrained or modified for a card.
 *
 * This system uses "Rule Families" to handle different data types (Sets, Ranges, Booleans).
 *
 * EXTENSIBILITY GUIDE:
 * To add a new rule for a filter field:
 * 1. Identify the field type (Set -> Categorical, Range -> Range, Boolean -> Toggle, Scalar -> Value).
 * 2. Add a property to [CardFilterRules] using the corresponding Rule Family.
 * 3. Update [normalize] to apply the rule and [layeredWith] to merge it.
 */
data class CardFilterRules(
    /** Rules for transaction types (e.g. Income, Expense, Transfer). */
    val typeRule: CategoricalRule<TransactionType> = CategoricalRule.Open(),

    /** Rules for account selection. */
    val accountRule: CategoricalRule<String> = CategoricalRule.Open(),

    /** Rules for category selection. */
    val categoryRule: CategoricalRule<Long> = CategoricalRule.Open(),

    /** Rules for transaction status. */
    val statusRule: CategoricalRule<com.osscabinet.ossWallet.domain.model.TransactionStatus> = CategoricalRule.Open(),

    /** Rules for the "Exclude Future" toggle. */
    val futureToggleRule: ToggleRule = ToggleRule.UserControlled,

    /** Rules for the amount range. */
    val amountRule: RangeRule<BigDecimal> = RangeRule.Open(),

    /** Rules for currency selection. */
    val currencyRule: CategoricalRule<String> = CategoricalRule.Open(),

    /** Rules for tag selection. */
    val tagRule: CategoricalRule<String> = CategoricalRule.Open(),

    /** Rules for payment method selection. */
    val paymentMethodRule: CategoricalRule<com.osscabinet.ossWallet.domain.model.PaymentMethod> = CategoricalRule.Open(),

    /** Rules for warranty selection. */
    val warrantyRule: ToggleRule = ToggleRule.UserControlled,

    /** Rules for linked records. */
    val linkedRecordsRule: ToggleRule = ToggleRule.UserControlled,

    /** Rules for searching (Query). Unified rule for note/payee fields. */
    val searchRule: ToggleRule = ToggleRule.UserControlled,

    /** Rules for timeline/date selection. */
    val timelineRule: ScalarRule<com.osscabinet.ossWallet.domain.model.timeline.TimelineRange> = ScalarRule.Open(),

    /** Flag for allowing tag creation. Relies on [tagRule] being editable. */
    val allowTagCreation: Boolean = true,

    /** Flag for allowing custom date ranges. Relies on [timelineRule] being editable. */
    val allowTimelineCustomRange: Boolean = true
) {
    /**
     * Normalizes a candidate filter against these rules.
     * This is the "Gatekeeper" that ensures data flowing into a card respects its constraints.
     */
    fun normalize(candidate: TransactionFilter): TransactionFilter {
        val normalizedTimeline = timelineRule.apply(candidate.timelineRange)
        val finalTimeline =
            normalizedTimeline.withExcludeFuture(futureToggleRule.apply(candidate.timelineRange.excludeFuture))

        return candidate.copy(
            transactionTypes = typeRule.apply(candidate.transactionTypes),
            accountIds = accountRule.apply(candidate.accountIds),
            categoryIds = categoryRule.apply(candidate.categoryIds),
            statuses = statusRule.apply(candidate.statuses),
            minAmount = amountRule.applyMin(candidate.minAmount),
            maxAmount = amountRule.applyMax(candidate.maxAmount),
            currencies = currencyRule.apply(candidate.currencies),
            tags = tagRule.apply(candidate.tags),
            paymentMethods = paymentMethodRule.apply(candidate.paymentMethods),
            timelineRange = finalTimeline,
            queryNote = if (searchRule.isVisible) candidate.queryNote else null,
            queryPayee = if (searchRule.isVisible) candidate.queryPayee else null,
            hasWarranty = if (warrantyRule.isVisible) candidate.hasWarranty else null,
            linkedTransactionIds = if (linkedRecordsRule.isVisible) candidate.linkedTransactionIds else emptySet()
        )
    }

    /**
     * Layers this rule set with external [constraints].
     * Generally used when a card is placed in a restricted context (e.g. an Account screen).
     * The "Most Restrictive Rule Wins" principle is applied during merging.
     */
    fun layeredWith(constraints: CardFilterRules): CardFilterRules {
        return this.copy(
            typeRule = typeRule.merge(constraints.typeRule),
            accountRule = accountRule.merge(constraints.accountRule),
            categoryRule = categoryRule.merge(constraints.categoryRule),
            statusRule = statusRule.merge(constraints.statusRule),
            futureToggleRule = futureToggleRule.merge(constraints.futureToggleRule),
            amountRule = amountRule.merge(constraints.amountRule),
            currencyRule = currencyRule.merge(constraints.currencyRule),
            tagRule = tagRule.merge(constraints.tagRule),
            paymentMethodRule = paymentMethodRule.merge(constraints.paymentMethodRule),
            warrantyRule = warrantyRule.merge(constraints.warrantyRule),
            linkedRecordsRule = linkedRecordsRule.merge(constraints.linkedRecordsRule),
            searchRule = searchRule.merge(constraints.searchRule),
            timelineRule = timelineRule.merge(constraints.timelineRule),

            allowTagCreation = allowTagCreation && constraints.allowTagCreation,
            allowTimelineCustomRange = allowTimelineCustomRange && constraints.allowTimelineCustomRange
        )
    }

    companion object {
        val DEFAULT = CardFilterRules()

        /** Rules for cards where category selection is locked. */
        val CATEGORY_SELECTION_LOCKED = CardFilterRules(
            categoryRule = CategoricalRule.Disabled()
        )

        /** Constraint profile for Account-scoped screens. */
        val ACCOUNT_LOCKED = CardFilterRules(
            accountRule = CategoricalRule.Disabled()
        )
    }
}

// --- RULE FAMILIES ---

/**
 * Logic for discrete sets of values (e.g. Accounts, Categories, TransactionTypes).
 */
sealed class CategoricalRule<T> {
    abstract fun apply(current: Set<T>): Set<T>
    abstract fun merge(other: CategoricalRule<T>): CategoricalRule<T>

    abstract val isLocked: Boolean
    abstract val lockedValues: Set<T>

    open val isUserEditable: Boolean get() = !isLocked
    open val isVisible: Boolean get() = true

    /** No restrictions. */
    class Open<T> : CategoricalRule<T>() {
        override fun apply(current: Set<T>) = current
        override fun merge(other: CategoricalRule<T>) = other
        override val isLocked: Boolean = false
        override val lockedValues: Set<T> = emptySet()
    }

    /** User selection is used, but [forbidden] items are always stripped out. */
    data class Filtered<T>(val forbidden: Set<T>) : CategoricalRule<T>() {
        override fun apply(current: Set<T>) = current.minus(forbidden)
        override fun merge(other: CategoricalRule<T>): CategoricalRule<T> = when (other) {
            is Open -> this
            is Filtered -> Filtered(forbidden.union(other.forbidden))
            is Limited -> Limited(other.allowed.minus(forbidden))
            is Locked -> Locked(other.forced.minus(forbidden))
            is Disabled -> other
        }

        override val isLocked: Boolean = false
        override val lockedValues: Set<T> = emptySet()
    }

    /** User can only pick from [allowed]. Results in intersection of user choice and [allowed]. */
    data class Limited<T>(val allowed: Set<T>) : CategoricalRule<T>() {
        override fun apply(current: Set<T>): Set<T> {
            return if (current.isEmpty()) allowed else current.intersect(allowed)
        }

        override fun merge(other: CategoricalRule<T>): CategoricalRule<T> = when (other) {
            is Open -> this
            is Filtered -> Limited(allowed.minus(other.forbidden))
            is Limited -> Limited(allowed.intersect(other.allowed))
            is Locked -> Locked(other.forced.intersect(allowed))
            is Disabled -> other
        }

        override val isLocked: Boolean = false
        override val lockedValues: Set<T> = emptySet()
    }

    /** Hard override. The selection is always exactly [forced] regardless of input. */
    data class Locked<T>(val forced: Set<T>) : CategoricalRule<T>() {
        override fun apply(current: Set<T>) = forced
        override fun merge(other: CategoricalRule<T>): CategoricalRule<T> = when (other) {
            is Locked -> Locked(forced.intersect(other.forced))
            is Disabled -> other
            else -> this
        }

        override val isLocked: Boolean = true
        override val lockedValues: Set<T> = forced
    }

    /** Effectively acts as "Hide All" or "Ignore Filter" for this field. */
    class Disabled<T> : CategoricalRule<T>() {
        override fun apply(current: Set<T>) = emptySet<T>()
        override fun merge(other: CategoricalRule<T>) = this
        override val isLocked: Boolean = true
        override val lockedValues: Set<T> = emptySet()
        override val isUserEditable: Boolean = false
        override val isVisible: Boolean = false
    }
}

/**
 * Logic for continuous ranges of values (e.g. Amounts, Dates).
 */
sealed class RangeRule<T : Comparable<T>> {
    abstract fun applyMin(current: T?): T?
    abstract fun applyMax(current: T?): T?
    abstract fun merge(other: RangeRule<T>): RangeRule<T>

    abstract val isLocked: Boolean
    open val isUserEditable: Boolean get() = !isLocked
    open val isVisible: Boolean get() = true

    class Open<T : Comparable<T>> : RangeRule<T>() {
        override fun applyMin(current: T?) = current
        override fun applyMax(current: T?) = current
        override fun merge(other: RangeRule<T>) = other
        override val isLocked: Boolean = false
    }

    /** Ensures the range stays within [minBoundary] and [maxBoundary]. */
    data class Clamped<T : Comparable<T>>(val minBoundary: T?, val maxBoundary: T?) :
        RangeRule<T>() {
        override fun applyMin(current: T?): T? {
            return when {
                minBoundary == null -> current
                current == null -> minBoundary
                else -> if (current < minBoundary) minBoundary else current
            }
        }

        override fun applyMax(current: T?): T? {
            return when {
                maxBoundary == null -> current
                current == null -> maxBoundary
                else -> if (current > maxBoundary) maxBoundary else current
            }
        }

        override fun merge(other: RangeRule<T>): RangeRule<T> = when (other) {
            is Open -> this
            is Clamped -> Clamped(
                minBoundary = mergeMin(minBoundary, other.minBoundary),
                maxBoundary = mergeMax(maxBoundary, other.maxBoundary)
            )

            is Fixed -> other.merge(this)
            is Disabled -> other
        }

        private fun mergeMin(a: T?, b: T?): T? = when {
            a == null -> b
            b == null -> a
            else -> if (a > b) a else b
        }

        private fun mergeMax(a: T?, b: T?): T? = when {
            a == null -> b
            b == null -> a
            else -> if (a < b) a else b
        }

        override val isLocked: Boolean = false
    }

    /** Hard override. The range is always exactly [min] to [max]. */
    data class Fixed<T : Comparable<T>>(val min: T?, val max: T?) : RangeRule<T>() {
        override fun applyMin(current: T?) = min
        override fun applyMax(current: T?) = max
        override fun merge(other: RangeRule<T>) = when (other) {
            is Disabled -> other
            else -> this
        }

        override val isLocked: Boolean = true
    }

    /** Effectively acts as "Hide All" or "Ignore Filter" for this field. */
    class Disabled<T : Comparable<T>> : RangeRule<T>() {
        override fun applyMin(current: T?) = null
        override fun applyMax(current: T?) = null
        override fun merge(other: RangeRule<T>) = this
        override val isLocked: Boolean = true
        override val isUserEditable: Boolean = false
        override val isVisible: Boolean = false
    }
}

/**
 * Logic for boolean flags (e.g. Exclude Future).
 */
sealed class ToggleRule {
    abstract fun apply(current: Boolean): Boolean
    abstract fun merge(other: ToggleRule): ToggleRule

    abstract val isLocked: Boolean
    open val isUserEditable: Boolean get() = !isLocked
    open val isVisible: Boolean get() = true

    object UserControlled : ToggleRule() {
        override fun apply(current: Boolean) = current
        override fun merge(other: ToggleRule) = other
        override val isLocked: Boolean = false
    }

    data class Forced(val value: Boolean) : ToggleRule() {
        override fun apply(current: Boolean) = value
        override fun merge(other: ToggleRule) = when (other) {
            is Disabled -> other
            else -> this
        }

        override val isLocked: Boolean = true
    }

    object Disabled : ToggleRule() {
        override fun apply(current: Boolean) = false
        override fun merge(other: ToggleRule) = this
        override val isLocked: Boolean = true
        override val isUserEditable: Boolean = false
        override val isVisible: Boolean = false
    }
}

/**
 * Logic for scalar values (e.g. Search Query, Timeline Range).
 */
sealed class ScalarRule<T> {
    abstract fun apply(current: T): T
    abstract fun merge(other: ScalarRule<T>): ScalarRule<T>

    abstract val isLocked: Boolean
    open val isUserEditable: Boolean get() = !isLocked
    open val isVisible: Boolean get() = true

    class Open<T> : ScalarRule<T>() {
        override fun apply(current: T) = current
        override fun merge(other: ScalarRule<T>) = other
        override val isLocked: Boolean = false
    }

    data class Locked<T>(val forced: T) : ScalarRule<T>() {
        override fun apply(current: T) = forced
        override fun merge(other: ScalarRule<T>) = when (other) {
            is Disabled -> other
            else -> this
        }

        override val isLocked: Boolean = true
    }

    class Disabled<T>(val defaultValue: T) : ScalarRule<T>() {
        override fun apply(current: T) = defaultValue
        override fun merge(other: ScalarRule<T>) = this
        override val isLocked: Boolean = true
        override val isUserEditable: Boolean = false
        override val isVisible: Boolean = false
    }
}
