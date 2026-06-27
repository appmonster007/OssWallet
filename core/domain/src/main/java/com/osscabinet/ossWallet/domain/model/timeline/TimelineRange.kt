package com.osscabinet.ossWallet.domain.model.timeline

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
enum class ThisPeriodType(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    QUARTER("This Quarter"),
    HALF_YEAR("This Half"),
    YEAR("This Year")
}

@Serializable
enum class LastPeriodType(val label: String) {
    LAST_24H("24 Hours"),
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    LAST_12_WEEKS("12 Weeks"),
    LAST_6_MONTHS("6 Months"),
    LAST_1_YEAR("1 Year"),
    LAST_5_YEARS("5 Years")
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("timeline_type")
sealed class TimelineRange {
    abstract val excludeFuture: Boolean

    @Serializable
    data class AllTime(override val excludeFuture: Boolean = true) : TimelineRange()
    @Serializable
    data class ThisPeriod(val period: ThisPeriodType, val offset: Int = 0, override val excludeFuture: Boolean = false) : TimelineRange()
    @Serializable
    data class LastPeriod(val period: LastPeriodType, val offset: Int = 0, override val excludeFuture: Boolean = false) : TimelineRange()
    @Serializable
    data class CustomRange(val start: Long? = null, val end: Long? = null, override val excludeFuture: Boolean = false) : TimelineRange()

    fun toDisplayString(): String = when (this) {
        is AllTime -> "All Time"
        is ThisPeriod -> period.label
        is LastPeriod -> "Last ${period.label}"
        is CustomRange -> {
            if (start != null && end != null) {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd", java.util.Locale.getDefault())
                val zone = java.time.ZoneId.systemDefault()
                val fmt = { ms: Long -> formatter.format(java.time.Instant.ofEpochMilli(ms).atZone(zone)) }
                "${fmt(start)} - ${fmt(end)}"
            } else "Custom"
        }
    }

    /**
     * Creates a copy of this range with a different excludeFuture setting.
     */
    fun withExcludeFuture(exclude: Boolean): TimelineRange = when (this) {
        is AllTime -> copy(excludeFuture = exclude)
        is ThisPeriod -> copy(excludeFuture = exclude)
        is LastPeriod -> copy(excludeFuture = exclude)
        is CustomRange -> copy(excludeFuture = exclude)
    }
}
