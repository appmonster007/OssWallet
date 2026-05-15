package com.osscabinet.ossWallet.domain.model.timeline

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("timeline_type")
sealed class TimelineRange {
    @Serializable
    object AllTime : TimelineRange()
    @Serializable
    data class ThisPeriod(val period: ThisPeriodType, val offset: Int = 0) : TimelineRange()
    @Serializable
    data class LastPeriod(val period: LastPeriodType, val offset: Int = 0) : TimelineRange()
    @Serializable
    data class CustomRange(val start: Long? = null, val end: Long? = null) : TimelineRange()

    fun toDisplayString(): String = when (this) {
        is AllTime -> "All Time"
        is ThisPeriod -> period.label
        is LastPeriod -> "Last ${period.label}"
        is CustomRange -> {
            if (start != null && end != null) {
                val formatter = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                "${formatter.format(java.util.Date(start))} - ${formatter.format(java.util.Date(end))}"
            } else "Custom"
        }
    }
}

@Serializable
enum class ThisPeriodType(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    QUARTER("This Quarter"),
    HALF_YEAR("Half Year"),
    YEAR("This Year")
}

@Serializable
enum class LastPeriodType(val label: String) {
    LAST_24H("24h"),
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    LAST_12_WEEKS("12 Weeks"),
    LAST_6_MONTHS("6 Months"),
    LAST_1_YEAR("1 Year"),
    LAST_5_YEARS("5 Years")
}
